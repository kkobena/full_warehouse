import {Directive, ElementRef, NgZone, OnInit, input, inject, HostListener} from '@angular/core';
import { EditableColumn, Table } from 'primeng/table';

@Directive({
  selector: '[jhiTableEditor]',
})
export class TableEditorDirective  {
  constructor(
    dt: Table, el: ElementRef, zone: NgZone,
  ) {

  }

  @HostListener('keydown.tab', ['$event'])
  @HostListener('keydown.shift.tab', ['$event'])
  @HostListener('keydown.meta.tab', ['$event'])
  @HostListener('keydown.enter', ['$event'])
  onShiftKeyDown(event: KeyboardEvent) {

  }
}
