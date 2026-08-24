export type AppDataGridSize = 'small' | 'normal' | 'large';
export type AppDataGridSelectionMode = 'single' | 'multiple' | null;
export type AppDataGridSortOrder = 1 | -1;
export type AppDataGridEditStartSource = 'click' | 'double-click' | 'keyboard' | 'backspace';
export type AppDataGridEditStopSource = 'enter' | 'tab' | 'blur' | 'escape';

export interface AppDataGridEditorOptions {
  readonly min?: number;
  readonly max?: number;
  readonly step?: number;
  readonly maxLength?: number;
  readonly selectOnEdit?: boolean;
  readonly validate?: (value: unknown) => string | null;
}

export interface AppDataGridColumn<T> {
  readonly id: string;
  readonly field?: keyof T & string;
  readonly header: string;
  readonly headerTooltip?: string;
  readonly type?: 'text' | 'number' | 'date' | 'boolean' | 'actions';

  readonly width?: number;
  readonly minWidth?: number;
  readonly maxWidth?: number;
  readonly flex?: number;
  readonly align?: 'left' | 'center' | 'right';
  readonly pinned?: 'left' | 'right';
  readonly hidden?: boolean;
  readonly sortable?: boolean;
  /** `false` verrouille la colonne à `width` (ou `minWidth`, 120 px par défaut). */
  readonly resizable?: boolean;
  /** Active l'édition si la colonne possède aussi un `field`. */
  readonly editable?: boolean | ((context: AppDataGridCellContext<T>) => boolean);
  /** Par défaut, reprend `type` si celui-ci vaut `text` ou `number`. */
  readonly editor?: 'text' | 'number';
  readonly editorOptions?: AppDataGridEditorOptions;

  readonly value?: (context: AppDataGridRowContext<T>) => unknown;
  readonly format?: (context: AppDataGridCellContext<T>) => string;
  readonly cellClass?: string | ((context: AppDataGridCellContext<T>) => string | readonly string[] | null);
  readonly tooltip?: string | ((context: AppDataGridCellContext<T>) => string | null);
}

export interface AppDataGridRowContext<T> {
  readonly row: T;
  readonly rowIndex: number;
  readonly rowKey: PropertyKey;
  readonly selected: boolean;
  readonly expanded: boolean;
}

export interface AppDataGridCellContext<T> extends AppDataGridRowContext<T> {
  readonly column: AppDataGridColumn<T>;
  readonly value: unknown;
  readonly toggleSelection: () => void;
  readonly toggleDetail: () => void;
}

export interface AppDataGridDetailContext<T> extends AppDataGridRowContext<T> {
  readonly collapse: () => void;
}

export interface AppDataGridCellTemplateContext<T> extends AppDataGridCellContext<T> {
  readonly $implicit: AppDataGridCellContext<T>;
}

export interface AppDataGridDetailTemplateContext<T> extends AppDataGridDetailContext<T> {
  readonly $implicit: AppDataGridDetailContext<T>;
}

export interface AppDataGridCellEvent<T> {
  readonly row: T;
  readonly rowKey: PropertyKey;
  readonly rowIndex: number;
  readonly column: AppDataGridColumn<T>;
  readonly originalEvent: MouseEvent;
}

export interface AppDataGridRowEvent<T> {
  readonly row: T;
  readonly rowKey: PropertyKey;
  readonly rowIndex: number;
  readonly originalEvent: MouseEvent;
}

export interface AppDataGridSortEvent {
  readonly columnId: string;
  readonly field?: string;
  readonly order: AppDataGridSortOrder;
}

export interface AppDataGridDetailToggleEvent<T> {
  readonly row: T;
  readonly rowKey: PropertyKey;
  readonly expanded: boolean;
}

export interface AppDataGridSortState {
  readonly columnId: string;
  readonly order: AppDataGridSortOrder;
}

export interface AppDataGridColumnResizeEvent {
  readonly columnId: string;
  readonly width: number;
  readonly source: 'pointer' | 'keyboard' | 'reset';
}

export interface AppDataGridCellEditingStartedEvent<T> {
  readonly row: T;
  readonly rowKey: PropertyKey;
  readonly rowIndex: number;
  readonly column: AppDataGridColumn<T>;
  readonly oldValue: unknown;
  readonly source: AppDataGridEditStartSource;
  readonly originalEvent: MouseEvent | KeyboardEvent;
}

export interface AppDataGridCellValueChangedEvent<T> {
  readonly row: T;
  readonly rowKey: PropertyKey;
  readonly rowIndex: number;
  readonly column: AppDataGridColumn<T>;
  readonly field: keyof T & string;
  readonly oldValue: unknown;
  readonly newValue: unknown;
  readonly source: Exclude<AppDataGridEditStopSource, 'escape'>;
}

export interface AppDataGridCellEditingStoppedEvent<T> {
  readonly row: T;
  readonly rowKey: PropertyKey;
  readonly rowIndex: number;
  readonly column: AppDataGridColumn<T>;
  readonly committed: boolean;
  readonly source: AppDataGridEditStopSource;
}
