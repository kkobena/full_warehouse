import { Injectable } from '@angular/core';
import { Observable, Observer, Subscription } from 'rxjs';
import { filter, share } from 'rxjs/operators';

export class SaleEvent<T> {
  constructor(
    public name: string,
    public content: T,
  ) {}
}

/**
 * An utility class to manage RX events
 */
@Injectable({
  providedIn: 'root',
})
export class SaleEventManager {
  observable: Observable<SaleEvent<any>>;
  observer?: Observer<SaleEvent<any>>;

  constructor() {
    this.observable = new Observable((observer: Observer<SaleEvent<any>>) => {
      this.observer = observer;
    }).pipe(share());
  }

  /**
   * Method to broadcast the event to observer
   */
  broadcast(event: SaleEvent<any>): void {
    if (this.observer) {
      this.observer.next(event);
    }
  }

  /**
   * Method to subscribe to an event with callback
   * @param eventNames  Single event name or array of event names to what subscribe
   * @param callback    Callback to run when the event occurs
   */
  subscribe(eventNames: string | string[], callback: (event: SaleEvent<any>) => void): Subscription {
    if (typeof eventNames === 'string') {
      eventNames = [eventNames];
    }
    return this.observable
      .pipe(
        filter((event: SaleEvent<any>) => {
          for (const eventName of eventNames) {
            if ((typeof event === 'string' && event === eventName) || (typeof event !== 'string' && event.name === eventName)) {
              return true;
            }
          }
          return false;
        }),
      )
      .subscribe(callback);
  }

  /**
   * Method to unsubscribe the subscription
   */
  destroy(subscriber: Subscription): void {
    subscriber.unsubscribe();
  }
}
