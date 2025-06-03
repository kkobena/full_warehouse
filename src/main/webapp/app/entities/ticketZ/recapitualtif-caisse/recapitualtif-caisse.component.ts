import { Component, inject } from '@angular/core';
import { RecapitulatifCaisseService } from '../recapitulatif-caisse.service';
import { TIMES } from '../../../shared/util/times';

@Component({
  selector: 'jhi-recapitualtif-caisse',
  imports: [],
  templateUrl: './recapitualtif-caisse.component.html',
  styles: ``,
})
export class RecapitualtifCaisseComponent {
  protected fromDate = new Date();
  protected toDate = new Date();
  protected fromTime = '00:00';
  protected toTime = '23:59';
  protected readonly times = TIMES;

  private readonly recapitulatifCaisseService = inject(RecapitulatifCaisseService);
}
