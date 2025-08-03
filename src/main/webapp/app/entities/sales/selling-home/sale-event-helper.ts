import { effect } from '@angular/core';

import { SaleEventSignal } from './sale-event';

export function handleSaleEvents(eventManager: SaleEventSignal, names: string | string[], callback: (event: any) => void): void {
  const filtered = eventManager.filterEvents(names);
  effect(() => {
    const events = filtered();
    if (events.length > 0) {
      for (const event of events) {
        callback(event);
      }
      eventManager.clear();
    }
  });
}
