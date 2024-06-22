import { Component, inject } from '@angular/core';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { KeyFilterModule } from 'primeng/keyfilter';
import { ToastModule } from 'primeng/toast';
import { DropdownModule } from 'primeng/dropdown';
import { CalendarModule } from 'primeng/calendar';
import { InputSwitchModule } from 'primeng/inputswitch';
import { ConfirmationService, MessageService } from 'primeng/api';
import { CashRegisterService } from './cash-register.service';
import { AccountService } from '../../core/auth/account.service';

@Component({
  selector: 'jhi-cash-register',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ConfirmDialogModule,
    DialogModule,
    ToolbarModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    RouterModule,
    TableModule,
    TooltipModule,
    KeyFilterModule,
    ToastModule,
    DropdownModule,
    CalendarModule,
    InputSwitchModule,
  ],
  templateUrl: './cash-register.component.html',
  styleUrl: './cash-register.component.scss',
})
export class CashRegisterComponent {
  currentAccount = inject(AccountService).trackCurrentAccount();

  constructor(
    protected entityService: CashRegisterService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    private messageService: MessageService,
    protected modalService: ConfirmationService,
    private fb: FormBuilder,
  ) {}
}
