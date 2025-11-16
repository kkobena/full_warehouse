import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-titlebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './titlebar.component.html',
  styleUrls: ['./titlebar.component.scss'],
})
export class TitlebarComponent implements OnInit {
  title = 'PharmaSmart';
  currentRoute = '';
  isMaximized = false;
  isTauri = false;
  private readonly router = inject(Router);

  async ngOnInit(): Promise<void> {
    // Check if running in Tauri
    if (typeof window !== 'undefined' && '__TAURI__' in window) {
      this.isTauri = true;

      // Import Tauri APIs dynamically
      const { getCurrentWindow } = await import('@tauri-apps/api/window');
      const appWindow = getCurrentWindow();

      // Check initial maximize state
      this.isMaximized = await appWindow.isMaximized();

      // Listen for resize events to update maximize state
      await appWindow.listen('tauri://resize', async () => {
        this.isMaximized = await appWindow.isMaximized();
      });
    }

    // Track current route for displaying in titlebar
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe((event: any) => {
      this.currentRoute = this.getRouteTitle(event.url);
    });
  }

  async minimizeWindow(): Promise<void> {
    if (this.isTauri) {
      const { getCurrentWindow } = await import('@tauri-apps/api/window');
      await getCurrentWindow().minimize();
    }
  }

  async maximizeWindow(): Promise<void> {
    if (this.isTauri) {
      const { getCurrentWindow } = await import('@tauri-apps/api/window');
      const appWindow = getCurrentWindow();

      if (this.isMaximized) {
        await appWindow.unmaximize();
      } else {
        await appWindow.maximize();
      }
    }
  }

  async closeWindow(): Promise<void> {
    if (this.isTauri) {
      const { getCurrentWindow } = await import('@tauri-apps/api/window');
      await getCurrentWindow().close();
    }
  }

  async openNewInstance(): Promise<void> {
    if (!this.isTauri) {
      return;
    }

    try {
      const { WebviewWindow } = await import('@tauri-apps/api/webviewWindow');

      // Generate unique label for new window
      const timestamp = Date.now();
      const label = `pharmasmart-${timestamp}`;

      // Create new window with same configuration
      const newWindow = new WebviewWindow(label, {
        url: '/',
        title: 'PharmaSmart',
        width: 1280,
        height: 800,
        minWidth: 1024,
        minHeight: 768,
        center: true,
        decorations: false,
        resizable: true,
      });

      // Listen for window creation
      newWindow.once('tauri://created', () => {
        console.log('New instance created:', label);
      });

      // Listen for errors
      newWindow.once('tauri://error', error => {
        console.error('Failed to create new instance:', error);
      });
    } catch (error) {
      console.error('Error opening new instance:', error);
    }
  }

  private getRouteTitle(url: string): string {
    // Remove query parameters and hash
    const cleanUrl = url.split('?')[0].split('#')[0];

    // Extract route name from URL
    const pathSegments = cleanUrl.split('/').filter(segment => segment);
    const mainRoute = pathSegments[0] || 'home';

    // Map route names to friendly display names
    const routeNameMap: { [key: string]: string } = {
      home: 'Accueil',
      dashboard: 'Tableau de bord',
      sales: 'Ventes',
      customers: 'Clients',
      products: 'Produits',
      inventory: 'Inventaire',
      orders: 'Commandes',
      suppliers: 'Fournisseurs',
      invoices: 'Factures',
      reports: 'Rapports',
      settings: 'Paramètres',
      users: 'Utilisateurs',
      'cash-register': 'Caisse',
      stock: 'Stock',
      ajustement: 'Ajustement',
      commande: 'Commande',
      facturation: 'Facturation',
      // Add more route mappings as needed
    };

    // Return mapped name or formatted route name
    return routeNameMap[mainRoute] || mainRoute.charAt(0).toUpperCase() + mainRoute.slice(1).replace(/-/g, ' ');
  }
}
