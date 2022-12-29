import {IOrderLine} from '../model/order-line.model';

export const formatNumberToString = (number: any): string => {
  return Math.floor(number.value)
    .toString()
    .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
};
export const checkIfAlineToBeUpdated = (orderLine: IOrderLine): boolean => {
  return (
    //  orderLine.provisionalCode ||
    !(orderLine.regularUnitPrice === orderLine.orderUnitPrice) || !(orderLine.orderCostAmount === orderLine.costAmount)
  );
};
