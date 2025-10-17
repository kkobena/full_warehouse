import { Component } from '@angular/core';

declare global {
  interface Window {
    ipcRenderer: any;
  }
}

@Component({
  selector: 'jhi-title-bar',
  templateUrl: './title-bar.component.html',
  styleUrls: ['./title-bar.component.scss'],
  standalone: true,
})
export class TitleBarComponent {
  minimize(): void {
    window.ipcRenderer.send('window-minimize');
  }

  maximize(): void {
    window.ipcRenderer.send('window-maximize');
  }

  close(): void {
    window.ipcRenderer.send('window-close');
  }
}