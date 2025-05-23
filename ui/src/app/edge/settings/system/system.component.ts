// @ts-strict-ignore
import { Component, effect } from "@angular/core";
import { UserService } from "src/app/shared/service/user.service";
import { environment } from "src/environments";
import { Edge, Service, UserPermission, Utils } from "../../../shared/shared";

@Component({
  selector: SystemComponent.SELECTOR,
  templateUrl: "./system.component.html",
  standalone: false,
})
export class SystemComponent {

  private static readonly SELECTOR = "system";

  protected readonly environment = environment;
  protected readonly spinnerId: string = SystemComponent.SELECTOR;
  protected showLog: boolean = false;
  protected readonly ESTIMATED_REBOOT_TIME = 600; // Seconds till the openems service is restarted after update
  protected edge: Edge;
  protected restartTime: number = this.ESTIMATED_REBOOT_TIME;
  protected canSeeSystemRestart: boolean = false;

  constructor(
    protected utils: Utils,
    private service: Service,
    private userService: UserService,
  ) {
    effect(async () => {
      const user = this.userService.currentUser();
      this.edge = await this.service.currentEdge();
      this.canSeeSystemRestart = UserPermission.isAllowedToSeeSystemRestart(user, this.edge);
    });
  }
}
