import { Directive, ElementRef, NgZone, OnInit, input, inject } from '@angular/core';
import { EditableColumn, Table } from 'primeng/table';

@Directive({
  selector: '[jhiTableEditor]',
})
export class TableEditorDirective extends EditableColumn {
  dt: Table;
  el: ElementRef;
  zone: NgZone;

  readonly moveToNext = input<boolean>();
  readonly openCurrentCell = input<boolean>();
  readonly moveToPrevious = input<boolean>();

  constructor() {
    const dt = inject(Table);
    const el = inject(ElementRef);
    const zone = inject(NgZone);

    super(dt, el, zone);

    this.dt = dt;
    this.el = el;
    this.zone = zone;
  }

  // @HostListener('keydown.enter', ['$event'])
  // @HostListener('keydown.meta.enter', ['$event'])
  // @HostListener('keydown', ['$event'])
  onEnterKeyDown($event: KeyboardEvent) {
    // if ($event.key === 'Enter' || $event.key === 'enter') {
    console.log($event.code, $event.key);
    //   this.openCell();
    console.error(this.moveToNext(), this.openCurrentCell());
    this.el.nativeElement.classList.add('p-cell-editing');
    setTimeout(() => {
      const moveToNext = this.moveToNext();
      const openCurrentCell = this.openCurrentCell();
      console.error(moveToNext, openCurrentCell, 'wdosk');
      if (moveToNext) {
        this.moveToNextCell($event);
      }
      if (openCurrentCell) {
        this.openCell();
      }
      /* if (this.moveToPrevious) {
        this.moveToPreviousCell($event);
      }*/
    }, 50);

    // }

    // this.moveToNextCell($event);
    // super.onEnterKeyDown($event);
  }

  /* @HostListener('keydown.shift.enter', ['$event'])
   onEnterKeyShift(event: KeyboardEvent) {
     if (!this.isEnabled()) {
       return;
     }

     this.dt.handleRowClick({
       originalEvent: event,
       rowData: this.data,
       rowIndex: this.rowIndex,
     });
   }*/
}
