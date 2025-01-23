import { Directive, ElementRef, Input, NgZone, OnInit } from '@angular/core';
import { EditableColumn, Table } from 'primeng/table';

@Directive({
    selector: '[jhiTableEditor]',
    standalone: false
})
export class TableEditorDirective extends EditableColumn implements OnInit {
  @Input() moveToNext: boolean;
  @Input() openCurrentCell: boolean;
  @Input() moveToPrevious: boolean;

  constructor(public dt: Table, public el: ElementRef, public zone: NgZone) {
    super(dt, el, zone);
  }

  ngOnInit() {}

  // @HostListener('keydown.enter', ['$event'])
  // @HostListener('keydown.meta.enter', ['$event'])
  // @HostListener('keydown', ['$event'])
  onEnterKeyDown($event: KeyboardEvent) {
    // if ($event.key === 'Enter' || $event.key === 'enter') {
    console.log($event.code, $event.key);
    //   this.openCell();
    console.error(this.moveToNext, this.openCurrentCell);
    this.el.nativeElement.classList.add('p-cell-editing');
    setTimeout(() => {
      console.error(this.moveToNext, this.openCurrentCell, 'wdosk');
      if (this.moveToNext) {
        this.moveToNextCell($event);
      }
      if (this.openCurrentCell) {
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
