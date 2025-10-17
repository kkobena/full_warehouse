import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faMinus, faSquare, faTimes } from '@fortawesome/free-solid-svg-icons';

declare global {
  interface Window {
    electron?: {
      window?: {
        minimize: () => void;
        maximize: () => void;
        close: () => void;
      };
    };
  }
}

@Component({
  selector: 'jhi-titlebar',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  templateUrl: './titlebar.component.html',
  styleUrl: './titlebar.component.scss'
})
export default class TitlebarComponent {
  readonly faMinus = faMinus;
  readonly faSquare = faSquare;
  readonly faTimes = faTimes;

  isElectron = false;

  constructor() {
    this.isElectron = !!window.electron;
  }

  minimize(): void {
    if (window.electron?.window) {
      window.electron.window.minimize();
    }
  }

  maximize(): void {
    if (window.electron?.window) {
      window.electron.window.maximize();
    }
  }

  close(): void {
    if (window.electron?.window) {
      window.electron.window.close();
    }
  }
}
