import { AfterViewInit, Component, ElementRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { LoginService } from 'app/login/login.service';
import { AccountService } from 'app/core/auth/account.service';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { WarehouseCommonModule } from '../shared/warehouse-common/warehouse-common.module';
import { InputTextModule } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AppSettingsDialogComponent } from '../shared/settings/app-settings-dialog.component';
import { TauriPrinterService } from '../shared/services/tauri-printer.service';

@Component({
  selector: 'jhi-login',
  imports: [
    WarehouseCommonModule,
    InputTextModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    CardModule,
    ButtonModule,
    RippleModule,
    Password,
    ToggleSwitchModule,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export default class LoginComponent implements OnInit, AfterViewInit {
  username = viewChild.required<ElementRef>('username');

  authenticationError = signal(false);
  isTauri = signal(false);

  loginForm = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    rememberMe: new FormControl(false, { nonNullable: true, validators: [Validators.required] }),
  });

  private readonly accountService = inject(AccountService);
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  ngOnInit(): void {
    this.accountService.identity().subscribe(() => {
      if (this.accountService.isAuthenticated()) {
        this.router.navigate(['']);
      }
    });
    this.isTauri.set(this.tauriPrinterService.isRunningInTauri());
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.username().nativeElement.focus();
    }, 100);
  }

  login(): void {
    this.loginService.login(this.loginForm.getRawValue()).subscribe({
      next: () => {
        this.authenticationError.set(false);
        if (!this.router.currentNavigation()) {
          this.router.navigate(['']);
        }
      },
      error: () => this.authenticationError.set(true),
    });
  }

  openSettings(): void {
    this.modalService.open(AppSettingsDialogComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
      windowClass: 'settings-modal',
    });
  }
}
