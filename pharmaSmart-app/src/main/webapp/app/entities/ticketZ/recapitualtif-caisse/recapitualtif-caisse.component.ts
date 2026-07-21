import { Component, inject, OnDestroy, OnInit, signal, viewChild, WritableSignal, ChangeDetectionStrategy } from '@angular/core';
import { RecapitulatifCaisseService } from '../recapitulatif-caisse.service';
import { TIMES } from '../../../shared/util/times';
import { RecapParam } from '../model/recap-param.model';
import { NGB_DATE_TO_ISO, TODAY_NGB_DATE } from '../../../shared/util/warehouse-util';
import { CommonModule } from '@angular/common';
import { Ticket } from '../model/ticket.model';
import { UserService } from '../../../core/user/user.service';
import { IUser } from '../../../core/user/user.model';
import { HttpResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NgbDateStruct, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import {
  AppSplitButtonItem,
  ButtonComponent,
  MultiSelectComponent,
  SelectComponent,
  SplitButtonComponent,
} from '../../../shared/ui';
import { PharmaDatePickerComponent } from '../../../shared/date-picker/pharma-date-picker.component';
import { NotificationService } from '../../../shared/services/notification.service';
import { SpinnerComponent } from '../../../shared/spinner/spinner.component';
import { TauriPrinterService } from '../../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../../shared/util/tauri-util';
import { MagasinService } from '../../magasin/magasin.service';
import { IMagasin } from "../../../shared/model";
import { EMPTY, Subject } from 'rxjs';
import { catchError, finalize, map, takeUntil, tap } from 'rxjs/operators';

@Component({
  selector: 'app-recapitualtif-caisse',
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    SplitButtonComponent,
    SelectComponent,
    MultiSelectComponent,
    PharmaDatePickerComponent,
    NgbTooltip,
    SpinnerComponent,
  ],
  templateUrl: './recapitualtif-caisse.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./recapitualtif-caisse.component.scss'],
})
export class RecapitualtifCaisseComponent implements OnInit, OnDestroy {
  // range15 = Array.from({ length: 5 }, (_, i) => i + 1);
  // `pharma-date-picker` (ng-bootstrap) travaille en `NgbDateStruct`, là où `p-datepicker`
  // manipulait des `Date`. La conversion vers l'ISO passe par `NGB_DATE_TO_ISO`.
  protected fromDate: NgbDateStruct = TODAY_NGB_DATE();
  protected toDate: NgbDateStruct = TODAY_NGB_DATE();
  protected fromTime = '00:00';
  protected toTime = '23:59';
  protected readonly mvts = [
    { label: 'Les ventes uniquement', value: true },
    { label: 'Tous les mouvemente', value: false },
  ];
  protected exportMenus: AppSplitButtonItem[];
  protected messageBtn: AppSplitButtonItem[];
  protected onlyVente = false;
  protected ticketZ: Ticket = null;
  protected users: IUser[] = [];
  protected selectedUsersId: (number | null)[] = [null];
  protected readonly hous = TIMES;
  private readonly recapitulatifCaisseService = inject(RecapitulatifCaisseService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly userService = inject(UserService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly magasinService = inject(MagasinService);
  private readonly notificationService = inject(NotificationService);
  private hasValidEmail: WritableSignal<boolean> = signal<boolean>(false);
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.loadAllUsers();
    this.initializeMenus();
    this.checkEmailConfiguration();
    this.fetchTickets();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAllUsers(): void {
    this.userService
      .query()
      .pipe(
        map((res: HttpResponse<IUser[]>) => res.body || []),
        tap(users => {
          this.users = [{ id: null, abbrName: 'TOUT' }, ...users];
        }),
        catchError(error => {
          console.error('Error loading users:', error);
          this.users = [{ id: null, abbrName: 'TOUT' }];
          return EMPTY;
        }),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  printReceiptForTauri(param: RecapParam): void {
    this.recapitulatifCaisseService
      .getEscPosReceiptForTauri(param)
      .pipe(
        tap(async (escposData: ArrayBuffer) => {
          try {
            await this.tauriPrinterService.printEscPosFromBuffer(escposData);
            this.notificationService.success('Impression envoyée avec succès', 'Succès');
          } catch (error) {
            this.notificationService.error("Erreur lors de l'impression", 'Erreur');
          }
        }),
        catchError(error => {
          this.notificationService.error('Erreur lors de la récupération du reçu', 'Erreur');
          return EMPTY;
        }),
        finalize(() => this.spinner().hide()),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  protected exportToPdf(): void {
    this.spinner().show();
    this.recapitulatifCaisseService
      .exportToPdf(this.buildParams())
      .pipe(
        tap(blob => {
          if (this.tauriPrinterService.isRunningInTauri()) {
            handleBlobForTauri(blob, 'recapitulatif-caisse');
          } else {
            window.open(URL.createObjectURL(blob));
          }
        }),
        catchError(error => {
          this.notificationService.error("Erreur lors de l'export PDF", 'Erreur');
          return EMPTY;
        }),
        finalize(() => this.spinner().hide()),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  protected fetchTickets(): void {
    this.spinner().show();
    this.recapitulatifCaisseService
      .query(this.buildParams())
      .pipe(
        map(response => response.body),
        tap(ticketZ => {
          this.ticketZ = ticketZ;
        }),
        catchError(error => {
          this.notificationService.error('Erreur lors de la récupération des tickets', 'Erreur');
          return EMPTY;
        }),
        finalize(() => this.spinner().hide()),
        takeUntil(this.destroy$),
      )
      .subscribe();
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
      this.recapitulatifCaisseService
        .print(params)
        .pipe(
          tap(() => {
            this.notificationService.success('Impression lancée avec succès', 'Succès');
          }),
          catchError(error => {
            console.error('Error printing:', error);
            this.notificationService.error("Erreur lors de l'impression", 'Erreur');
            return EMPTY;
          }),
          finalize(() => this.spinner().hide()),
          takeUntil(this.destroy$),
        )
        .subscribe();
    }
  }

  private sentMail(): void {
    if (!this.hasValidEmail()) {
      this.notificationService.warning("Ajouter une adresse e-mail valide à l'Officine", 'Avertissement');
      return;
    }

    this.spinner().show();
    this.recapitulatifCaisseService
      .sendMail(this.buildParams())
      .pipe(
        tap(() => {
          this.notificationService.success('Email envoyé avec succès', 'Succès');
        }),
        catchError(error => {
          console.error('Error sending email:', error);
          this.notificationService.error("Erreur lors de l'envoi de l'email", 'Erreur');
          return EMPTY;
        }),
        finalize(() => this.spinner().hide()),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  private initializeMenus(): void {
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
      },
    ];
  }

  private checkEmailConfiguration(): void {
    this.magasinService
      .getCurrenttUserMagasin()
      .pipe(
        map((res: HttpResponse<IMagasin>) => res.body?.email || null),
        tap(email => {
          this.hasValidEmail.set(!!email);
        }),
        catchError(error => {
          console.error('Error checking email configuration:', error);
          this.hasValidEmail.set(false);
          return EMPTY;
        }),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  private buildParams(): RecapParam {
    const usersId = this.selectedUsersId.filter((id): id is number => id !== null);
    return {
      fromDate: NGB_DATE_TO_ISO(this.fromDate),
      toDate: NGB_DATE_TO_ISO(this.toDate),
      fromTime: this.fromTime + ':00',
      toTime: this.toTime + ':59',
      onlyVente: this.onlyVente,
      ...(usersId.length > 0 && { usersId }),
    };
  }
}
