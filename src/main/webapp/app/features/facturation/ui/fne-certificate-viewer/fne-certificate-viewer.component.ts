import { Component, inject, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ButtonModule } from 'primeng/button';
import { TauriPrinterService } from "../../../../shared/services/tauri-printer.service";


@Component({
  selector: 'app-fne-certificate-viewer',
  imports: [ButtonModule],
  templateUrl: './fne-certificate-viewer.component.html',
  styleUrls: ['./fne-certificate-viewer.component.scss'],
})
export class FneCertificateViewerComponent implements OnInit {
  tokenUrl: string;
  reference: string;

  protected safeUrl: SafeResourceUrl;
  protected isRunningInTauri = false;
  private readonly activeModal = inject(NgbActiveModal);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly tauriPrinterService = inject(TauriPrinterService);

  ngOnInit(): void {
    this.isRunningInTauri = this.tauriPrinterService.isRunningInTauri();

    if (this.isRunningInTauri) {
      void this.openInExternalBrowser();
      setTimeout(() => this.close(), 1500);
    } else {
      // Dans le navigateur web, afficher l'iframe
      this.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.tokenUrl);
    }
  }

  close(): void {
    this.activeModal.dismiss();
  }

  async openInExternalBrowser(): Promise<void> {
    if (this.isRunningInTauri) {
      try {
        const { open } = await import('@tauri-apps/plugin-shell');
        await open(this.tokenUrl);
      } catch (error) {}
    } else {
      window.open(this.tokenUrl, '_blank');
    }
  }
}
