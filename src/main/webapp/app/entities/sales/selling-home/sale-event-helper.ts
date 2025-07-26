// Final version of handleSaleEvents to avoid Injector injection error
import { effect, inject } from '@angular/core';

import {SaleEventSignal} from "./sale-event";

export function handleSaleEvents(
  eventManager: SaleEventSignal,
  names: string | string[],
  callback: (event: any) => void
): void {
  const filtered = eventManager.filterEvents(names);
  // effect must be called in an injection context, so this must be invoked inside component constructor or lifecycle
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
