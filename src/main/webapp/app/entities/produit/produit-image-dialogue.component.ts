import { Component, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';
import { IProduit } from 'app/shared/model/produit.model';
import { ProduitService } from './produit.service';
@Component({
  selector: 'jhi-produit-image-dialogue',
  templateUrl: './produit-image-dialogue.component.html',
  styleUrls: ['./produit-image-dialogue.component.scss'],
})
export class ProduitImageDialogueComponent implements OnInit {
  selectedFile?: File;
  produit?: IProduit;

  constructor(protected produitService: ProduitService, public activeModal: NgbActiveModal, protected eventManager: JhiEventManager) {}

  cancel(): void {
    this.activeModal.dismiss();
  }
  ngOnInit(): void {}
  onFileChanged(event: any): void {
    this.selectedFile = event.target.files[0];
  }
  uploadImage(id: number): void {
    const formData: FormData = new FormData();
    if (this.selectedFile != null) {
      formData.append('productimg', this.selectedFile, this.selectedFile.name);
      this.produitService.uploadFile(formData, id).subscribe(() => {
        this.eventManager.broadcast('produitListModification');
        this.activeModal.close();
      });
    }
  }
}
