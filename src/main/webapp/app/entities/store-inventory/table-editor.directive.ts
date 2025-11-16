import { Directive, ElementRef, HostListener, NgZone } from '@angular/core';
import { Table } from 'primeng/table';

@Directive({
  selector: '[jhiTableEditor]',
})
export class TableEditorDirective {
  constructor(dt: Table, el: ElementRef, zone: NgZone) {}

  @HostListener('keydown.tab', ['$event'])
  @HostListener('keydown.shift.tab', ['$event'])
  @HostListener('keydown.meta.tab', ['$event'])
  @HostListener('keydown.enter', ['$event'])
  onShiftKeyDown(event: KeyboardEvent) {}
}
