import { computed, Injectable, signal } from '@angular/core';

export class SaleEvent<T = any> {
  constructor(
    public name: string,
    public content: T
  ) {
  }
}

@Injectable({ providedIn: 'root' })
export class SaleEventSignal {
  private readonly _events = signal<SaleEvent[]>([]);

  readonly events = this._events.asReadonly();

  broadcast<T>(event: SaleEvent<T>): void {
    this._events.set([...this._events(), event]);
  }

  clear(): void {
    this._events.set([]);
  }

  filterEvents(names: string | string[]) {
    const nameArray = Array.isArray(names) ? names : [names];
    return computed(() =>
      this._events().filter(e => nameArray.includes(e.name))
    );
  }
}
