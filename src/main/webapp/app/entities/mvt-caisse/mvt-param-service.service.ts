import { Injectable, signal, WritableSignal } from '@angular/core';
import { MvtCaisseParams } from './mvt-caisse-util';

@Injectable({
  providedIn: 'root',
})
export class MvtParamServiceService {
  mvtCaisseParam: WritableSignal<MvtCaisseParams> = signal<MvtCaisseParams>(null);

  setMvtCaisseParam(mvtCaisseParams: MvtCaisseParams): void {
    this.mvtCaisseParam.set(mvtCaisseParams);
  }
}
