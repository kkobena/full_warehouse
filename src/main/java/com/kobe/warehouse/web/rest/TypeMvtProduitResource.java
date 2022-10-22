package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.service.dto.TypeMvtProduitDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class TypeMvtProduitResource {
    @GetMapping("/typeMvtProduit")
    public ResponseEntity<List<TypeMvtProduitDTO>> allTypeMvtsProduit() {
        return ResponseEntity.ok().body( EnumSet.allOf(TransactionType.class).stream().map(e->new TypeMvtProduitDTO(e.ordinal(),e.getValue())).sorted(Comparator.comparing(TypeMvtProduitDTO::getName)).collect(Collectors.toList()));
    }
}
