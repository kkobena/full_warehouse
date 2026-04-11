import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { NavApiService } from 'app/core/data-access/nav-api.service';
import { NavStore } from 'app/core/store/nav.store';
import { NotificationService } from 'app/shared/services/notification.service';
import { INavNode, NavReorderPayload } from 'app/shared/model/nav-item.model';

@Component({
  selector: 'app-nav-reorder',
  templateUrl: './nav-reorder.component.html',
  styleUrl: './nav-reorder.component.scss',
  imports: [CommonModule, DragDropModule, ButtonModule, TagModule],
})
export class NavReorderComponent implements OnInit {
  private readonly navApi = inject(NavApiService);
  private readonly navStore = inject(NavStore);
  private readonly notificationService = inject(NotificationService);

  protected readonly saving = signal(false);
  protected readonly hasUnsavedChanges = signal(false);
  protected readonly groups = signal<INavNode[]>([]);

  private originalGroups: INavNode[] = [];

  ngOnInit(): void {
    this.loadFromStore();
  }

  private loadFromStore(): void {
    const tree = this.navStore.navTree();
    if (tree.length) {
      this.setGroups(tree);
    } else {
      this.navApi.getAllNavItems().subscribe({
        next: items => this.setGroups(items),
        error: () => this.notificationService.error('Impossible de charger les items de navigation.'),
      });
    }
  }

  private setGroups(items: INavNode[]): void {
    this.originalGroups = JSON.parse(JSON.stringify(items));
    this.groups.set(JSON.parse(JSON.stringify(items)));
    this.hasUnsavedChanges.set(false);
  }

  onGroupDropped(event: CdkDragDrop<INavNode[]>): void {
    const current = [...this.groups()];
    moveItemInArray(current, event.previousIndex, event.currentIndex);
    this.groups.set(current);
    this.hasUnsavedChanges.set(true);
  }

  onChildDropped(event: CdkDragDrop<INavNode[]>, targetGroup: INavNode): void {
    const current = [...this.groups()];
    const targetIdx = current.findIndex(g => g.id === targetGroup.id);
    if (targetIdx === -1) return;

    if (event.previousContainer === event.container) {
      const children = [...(current[targetIdx].children ?? [])];
      moveItemInArray(children, event.previousIndex, event.currentIndex);
      current[targetIdx] = { ...current[targetIdx], children };
    } else {
      const sourceId = parseInt(event.previousContainer.id.replace('drop-', ''), 10);
      const sourceIdx = current.findIndex(g => g.id === sourceId);
      if (sourceIdx === -1) return;
      const sourceChildren = [...(current[sourceIdx].children ?? [])];
      const targetChildren = [...(current[targetIdx].children ?? [])];
      transferArrayItem(sourceChildren, targetChildren, event.previousIndex, event.currentIndex);
      current[sourceIdx] = { ...current[sourceIdx], children: sourceChildren };
      current[targetIdx] = { ...current[targetIdx], children: targetChildren };
    }

    this.groups.set(current);
    this.hasUnsavedChanges.set(true);
  }

  getConnectedLists(groupId: number): string[] {
    return this.groups()
      .filter(g => g.id !== groupId && g.children)
      .map(g => `drop-${g.id}`);
  }

  saveOrder(): void {
    this.saving.set(true);
    const payload: NavReorderPayload[] = this.buildReorderPayload();
    this.navApi.saveAdminReorder(payload).subscribe({
      next: () => {
        this.notificationService.success('Ordre enregistré pour tous les utilisateurs.');
        this.navStore.invalidate();
        this.originalGroups = JSON.parse(JSON.stringify(this.groups()));
        this.hasUnsavedChanges.set(false);
        this.saving.set(false);
      },
      error: () => {
        this.notificationService.error('Impossible d\'enregistrer l\'ordre.');
        this.saving.set(false);
      },
    });
  }

  resetOrder(): void {
    this.groups.set(JSON.parse(JSON.stringify(this.originalGroups)));
    this.hasUnsavedChanges.set(false);
  }

  private buildReorderPayload(): NavReorderPayload[] {
    const payload: NavReorderPayload[] = [];
    this.groups().forEach((group, gi) => {
      payload.push({ navItemId: group.id, newOrdre: gi, newParentId: null });
      group.children?.forEach((child, ci) => {
        payload.push({ navItemId: child.id, newOrdre: ci, newParentId: group.id });
      });
    });
    return payload;
  }
}

