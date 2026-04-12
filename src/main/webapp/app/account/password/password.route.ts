import { Route } from '@angular/router';
import { AuthGuard } from 'app/core/auth/auth.guard';
import PasswordComponent from './password.component';

const passwordRoute: Route = {
  path: 'password',
  component: PasswordComponent,
  title: 'global.menu.account.password',
  canActivate: [AuthGuard],
};

export default passwordRoute;
