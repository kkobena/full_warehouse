import { Component, inject, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { ProfileService } from './profile.service';
import SharedModule from 'app/shared/shared.module';

@Component({
    selector: 'jhi-page-ribbon',
    template: `
    @if (ribbonEnv$ | async; as ribbonEnv) {
      <div class="ribbon">
        <a href="" [jhiTranslate]="'global.ribbon.' + (ribbonEnv ?? '')">{{ { dev: 'Développement' }[ribbonEnv ?? ''] }}</a>
      </div>
    }
  `,
    styleUrls: ['./page-ribbon.component.scss'],
    imports: [SharedModule]
})
export default class PageRibbonComponent implements OnInit {
  ribbonEnv$?: Observable<string | undefined>;

  private profileService = inject(ProfileService);

  ngOnInit(): void {
    this.ribbonEnv$ = this.profileService.getProfileInfo().pipe(map(profileInfo => profileInfo.ribbonEnv));
  }
}
