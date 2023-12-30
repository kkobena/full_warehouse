import { Routes } from '@angular/router';

import passwordRoute from './password/password.route';
import passwordResetFinishRoute from './password-reset/finish/password-reset-finish.route';
import passwordResetInitRoute from './password-reset/init/password-reset-init.route';
import sessionsRoute from './sessions/sessions.route';
import settingsRoute from './settings/settings.route';

const accountRoutes: Routes = [passwordRoute, passwordResetFinishRoute, passwordResetInitRoute, sessionsRoute, settingsRoute];

export default accountRoutes;
