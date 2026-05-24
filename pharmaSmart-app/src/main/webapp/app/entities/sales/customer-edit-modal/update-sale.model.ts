import { ICustomer } from '../../../shared/model';
import { IThirdPartySaleLine } from '../../../shared/model/third-party-sale-line';

export class UpdateSale {
  id: number;
  customer: ICustomer;
  ayantDroit?: ICustomer;
  thirdPartySaleLines?: IThirdPartySaleLine[];
  initialValue: any;
  finalValue: any;
}
