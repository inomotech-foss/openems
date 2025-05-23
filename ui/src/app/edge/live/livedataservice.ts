// @ts-strict-ignore
import { Directive, Inject, OnDestroy } from "@angular/core";
import { RefresherCustomEvent } from "@ionic/angular";
import { filter, take, takeUntil } from "rxjs/operators";
import { v4 as uuidv4 } from "uuid";
import { PlatFormService } from "src/app/platform.service";
import { DataService } from "../../shared/components/shared/dataservice";
import { ChannelAddress, CurrentData, Edge, Service, Websocket } from "../../shared/shared";

@Directive()
export class LiveDataService extends DataService implements OnDestroy {

    private subscribeId: string | null = null;
    private subscribedChannelAddresses: ChannelAddress[] = [];

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(Service) protected service: Service,
    ) {
        super();

        this.service.getCurrentEdge().then((edge) => {
            edge.currentData.pipe(takeUntil(this.stopOnDestroy))
                .subscribe(() => this.lastUpdated.set(new Date()));
        });
    }

    public getValues(channelAddresses: ChannelAddress[], edge: Edge, componentId: string) {

        for (const channelAddress of channelAddresses) {
            this.subscribedChannelAddresses.push(channelAddress);
        }

        this.subscribeId = uuidv4();
        this.edge = edge;
        if (channelAddresses.length != 0) {
            edge.subscribeChannels(this.websocket, this.subscribeId, channelAddresses);
        }

        // call onCurrentData() with latest data
        edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
            const allComponents = this.currentValue.value.allComponents;
            for (const channelAddress of channelAddresses) {
                const ca = channelAddress.toString();
                allComponents[ca] = currentData.channel[ca];
            }

            this.currentValue.next({ allComponents: allComponents });
            this.lastUpdated.set(new Date());
        });
    }

    ngOnDestroy() {
        this.edge.unsubscribeFromChannels(this.websocket, this.subscribedChannelAddresses);
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    public unsubscribeFromChannels(channels: ChannelAddress[]) {
        this.lastUpdated.set(null);
        this.edge.unsubscribeFromChannels(this.websocket, channels);
    }

    public override refresh(ev: RefresherCustomEvent) {
        PlatFormService.handleRefresh();
    }

    /**
     * Gets the first valid --non null/undefined-- value for passed channels and unsubscribes afterwards
     *
     * @param channelAddresses the channel addresses
     * @returns the currentData for thes channelAddresses
     */
    public async subscribeAndGetFirstValidValueForChannels(channelAddresses: ChannelAddress[], componentId: string): Promise<CurrentData> {
        this.getValues(channelAddresses, this.edge, componentId);
        return new Promise<any>((res) => {
            this.currentValue.pipe(filter(el => channelAddresses.every(channel => {
                const component = el.allComponents[channel.toString()];
                return component != null; // Ensure the component is not null or undefined
            })), take(1)).subscribe((val) => {
                this.unsubscribeFromChannels(channelAddresses);
                const allComponents: {} = channelAddresses.reduce((arr, channel) => {
                    arr[channel.toString()] = val.allComponents[channel.toString()];
                    return arr;
                }, {});
                val.allComponents = allComponents;
                res(val);
            });
        });
    }

    /**
     * Gets the first valid --non null/undefined-- value for this channel
     *
     * @param channelAddress the channel address
     * @returns a non null/undefined value
     */
    public async getFirstValidValueForChannel<T = any>(channelAddress: ChannelAddress): Promise<T> {
        return new Promise<any>((res) => {
            this.currentValue.pipe(filter(el => el.allComponents[channelAddress.toString()] != null), take(1)).subscribe((val) => {
                res(val.allComponents[channelAddress.toString()]);
            });
        });
    }
}
