import { Component, inject } from '@angular/core';
import { RecapitulatifCaisseService } from '../recapitulatif-caisse.service';
import { TIMES } from '../../../shared/util/times';
import { RecapParam } from '../model/recap-param.model';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { NgxSpinnerService } from 'ngx-spinner';
import { CommonModule } from '@angular/common';
import { Ticket } from '../model/ticket.model';
import { UserService } from '../../../core/user/user.service';
import { IUser } from '../../../core/user/user.model';
import { HttpResponse } from '@angular/common/http';
import { Panel } from 'primeng/panel';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { SelectModule } from 'primeng/select';
import { Toolbar } from 'primeng/toolbar';
import { FormsModule } from '@angular/forms';
import { MultiSelectModule } from 'primeng/multiselect';

@Component({
  selector: 'jhi-recapitualtif-caisse',
  imports: [CommonModule, Panel, Button, DatePicker, FloatLabel, Toolbar, FormsModule, MultiSelectModule, SelectModule],
  templateUrl: './recapitualtif-caisse.component.html',
  styles: ``,
})
export class RecapitualtifCaisseComponent {
  protected fromDate = new Date();
  protected toDate = new Date();
  protected fromTime = '00:00';
  protected toTime = '23:59';
  protected readonly mvts = [
    { label: 'Les ventes uniquement', value: true },
    { label: 'Tous les mouvemente', value: false },
  ];
  protected onlyVente = false;
  protected ticketZ: Ticket = null;
  protected users: IUser[] = [];
  protected selectedUsersId: number[] = [];
  protected readonly hous = TIMES;
  private readonly recapitulatifCaisseService = inject(RecapitulatifCaisseService);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly userService = inject(UserService);

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<IUser[]>) => {
      if (res.body) {
        this.users = [{ id: null, abbrName: 'TOUT' }];
        this.users = [...this.users, ...res.body];
      }
    });
  }

  protected exportToPdf(): void {
    this.spinner.show();
    this.recapitulatifCaisseService.exportToPdf(this.buildParams()).subscribe({
      next: blod => {
        this.spinner.hide();
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
      },
      error: () => this.spinner.hide(),
    });
  }

  protected fetchTickets(): void {
    this.spinner.show();
    this.recapitulatifCaisseService.query(this.buildParams()).subscribe({
      next: response => {
        this.ticketZ = response.body;
        this.spinner.hide();
      },
      error: () => this.spinner.hide(),
    });
  }

  protected printTickets(): void {
    this.spinner.show();
    this.recapitulatifCaisseService.print(this.buildParams()).subscribe({
      next: () => {
        this.spinner.hide();
      },
      error: () => this.spinner.hide(),
    });
  }

  protected onSelectedUsersChange(): void {
    if (this.selectedUsersId.length === 0) {
      this.selectedUsersId = [null]; // Sélectionne l'option "TOUT" si aucune autre n'est sélectionnée
    }
  }

  private buildParams(): RecapParam {
    return {
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate),
      toDate: DATE_FORMAT_ISO_DATE(this.toDate),
      fromTime: this.fromTime + ':00', // Ajout de ':00' pour le format HH:mm:ss
      toTime: this.toTime + ':59', // Ajout de ':59' pour le format HH:mm:ss
      onlyVente: this.onlyVente,
      usersId: this.selectedUsersId.length > 0 ? this.selectedUsersId : null,
    };
  }
}
