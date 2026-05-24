import { Route } from '@angular/router';
import { AuthGuard } from 'app/core/auth/auth.guard';
import SettingsComponent from './settings.component';

const settingsRoute: Route = {
  path: 'settings',
  component: SettingsComponent,
  title: 'global.menu.account.settings',
  canActivate: [AuthGuard],
};

export default settingsRoute;
