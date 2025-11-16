import { Component, inject, OnInit, viewChild } from '@angular/core';
import { RecapitulatifCaisseService } from '../recapitulatif-caisse.service';
import { TIMES } from '../../../shared/util/times';
import { RecapParam } from '../model/recap-param.model';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { CommonModule } from '@angular/common';
import { Ticket } from '../model/ticket.model';
import { UserService } from '../../../core/user/user.service';
import { IUser } from '../../../core/user/user.model';
import { HttpResponse } from '@angular/common/http';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { SelectModule } from 'primeng/select';
import { Toolbar } from 'primeng/toolbar';
import { FormsModule } from '@angular/forms';
import { MultiSelectModule } from 'primeng/multiselect';
import { MenuItem } from 'primeng/api';
import { SplitButton } from 'primeng/splitbutton';
import { Tooltip } from 'primeng/tooltip';
import { Card } from 'primeng/card';
import { SpinnerComponent } from '../../../shared/spinner/spinner.component';
import { TauriPrinterService } from '../../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../../shared/util/tauri-util';
import { PaymentId } from '../../differes/model/new-reglement-differe.model';

@Component({
  selector: 'jhi-recapitualtif-caisse',
  imports: [
    CommonModule,
    Button,
    DatePicker,
    FloatLabel,
    Toolbar,
    FormsModule,
    MultiSelectModule,
    SelectModule,
    SplitButton,
    Tooltip,
    Card,
    SpinnerComponent,
  ],
  templateUrl: './recapitualtif-caisse.component.html',
  styleUrls: ['./recapitualtif-caisse.component.scss'],
})
export class RecapitualtifCaisseComponent implements OnInit {
  // range15 = Array.from({ length: 5 }, (_, i) => i + 1);
  protected fromDate = new Date();
  protected toDate = new Date();
  protected fromTime = '00:00';
  protected toTime = '23:59';
  protected readonly mvts = [
    { label: 'Les ventes uniquement', value: true },
    { label: 'Tous les mouvemente', value: false },
  ];
  protected exportMenus: MenuItem[];
  protected messageBtn: MenuItem[];
  protected onlyVente = false;
  protected ticketZ: Ticket = null;
  protected users: IUser[] = [];
  protected selectedUsersId: number[] = [null];
  protected readonly hous = TIMES;
  private readonly recapitulatifCaisseService = inject(RecapitulatifCaisseService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly userService = inject(UserService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  ngOnInit(): void {
    this.loadAllUsers();
    this.exportMenus = [
      {
        label: 'Imprimer',
        icon: 'pi pi-print',
        command: () => this.print(),
      },
      {
        label: 'PDF',
        icon: 'pi pi-file-excel',
        command: () => this.exportToPdf(),
      },
    ];

    this.messageBtn = [
      {
        label: 'Mail',
        icon: 'pi pi-inbox',
        command: () => this.sentMail(),
      } /*,
      {
        label: 'SMS',
        icon: 'pi pi-send',
        command: () => this.sentSms(),
      },*/,
    ];
  }

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<IUser[]>) => {
      if (res.body) {
        this.users = [{ id: null, abbrName: 'TOUT' }];
        this.users = [...this.users, ...res.body];
      }
    });
  }

  protected exportToPdf(): void {
    this.spinner().show();
    this.recapitulatifCaisseService.exportToPdf(this.buildParams()).subscribe({
      next: blob => {
        this.spinner().hide();
        if (this.tauriPrinterService.isRunningInTauri()) {
          handleBlobForTauri(blob, 'recapitulatif-caisse');
        } else {
          window.open(URL.createObjectURL(blob));
        }
      },
      error: () => this.spinner().hide(),
    });
  }

  protected fetchTickets(): void {
    this.spinner().show();
    this.recapitulatifCaisseService.query(this.buildParams()).subscribe({
      next: response => {
        this.ticketZ = response.body;
        this.spinner().hide();
      },
      error: () => this.spinner().hide(),
    });
  }

  protected onSelectedUsersChange(): void {
    if (this.selectedUsersId.length === 0) {
      this.selectedUsersId = [null]; // Sélectionne l'option "TOUT" si aucune autre n'est sélectionnée
    }
  }

  private sentSms(): void {
    // Logique pour envoyer un SMS
    // Vous pouvez appeler un service pour envoyer le SMS ici
    console.log('SMS envoyé');
  }

  private print(): void {
    this.spinner().show();
    const params = this.buildParams();
    if (this.tauriPrinterService.isRunningInTauri()) {
      this.printReceiptForTauri(params);
    } else {
      this.recapitulatifCaisseService.print(params).subscribe({
        next: () => {
          this.spinner().hide();
        },
        error: () => this.spinner().hide(),
      });
    }
  }

  private sentMail(): void {
    this.spinner().show();
    this.recapitulatifCaisseService.sendMail(this.buildParams()).subscribe({
      next: () => {
        this.spinner().hide();
      },
      error: () => this.spinner().hide(),
    });
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
  printReceiptForTauri(param: RecapParam): void {
    this.recapitulatifCaisseService.getEscPosReceiptForTauri(param).subscribe({
      next: async (escposData: ArrayBuffer) => {
        try {
          await this.tauriPrinterService.printEscPosFromBuffer(escposData);
        } catch (error) {}
      },
      error: () => {},
    });
  }
}
