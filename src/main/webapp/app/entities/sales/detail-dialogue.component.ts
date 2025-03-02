import { Component, OnInit } from '@angular/core';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-detail-dialogue',
  templateUrl: './detail-dialogue.component.html',
  styleUrls: ['./detail-dialogue.component.scss'],
  imports: [WarehouseCommonModule],
})
export class DetailDialogueComponent implements OnInit {
  ngOnInit(): void {}
}
