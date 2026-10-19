package com.kobe.warehouse.service.dto.exports;

/** Export produit sur place, prêt à partir dans la réponse : son contenu, le nom proposé au navigateur, son type. */
public record ContenuExportDTO(byte[] contenu, String nom, String typeMime) {}
