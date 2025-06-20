package com.kobe.warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.kobe.warehouse.service.dto.TvaEmbeded;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//@ConditionalOnProperty(name = "sarenov.act.convertion.enable", havingValue = "true")
public final class Util {

    private static final Logger log = LoggerFactory.getLogger(Util.class);

    private static final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    private Util() {
        //        Class<?> clazz = this.getClass();
        //        clazz.getDeclaredField("log");

    }

    public static List<TvaEmbeded> transformTvaEmbeded(String content) {
        if (StringUtils.isNotEmpty(content)) {
            try {
                return new ObjectMapper().readValue(content, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.debug("{0}", e);
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    public static String transformTvaEmbededToString(List<TvaEmbeded> tvaEmbededs) {
        if (!tvaEmbededs.isEmpty()) {
            try {
                return new ObjectMapper().writeValueAsString(tvaEmbededs);
            } catch (JsonProcessingException e) {
                log.debug("{0}", e);
                return null;
            }
        }

        return null;
    }

    public static boolean isValidPhoneNumber(String phoneNumberInput) {
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(phoneNumberInput, "CI");
            return phoneNumberUtil.isValidNumber(phoneNumber);
        } catch (NumberParseException e) {
            return false;
        }
    }
    /*
   1- à partir de l'identifiant clavis à la rod (liste des packs de la structure)
    2- Liste consolidée des structures qui comportent un identifiant clavis
         - à jouter un champs clavis (du fichier open data)
         - appel d'une api côte rod
         - à la saisie de dans bddrenove (clavis), un appel sera fait à rod pour envoyer les infos de la structure
         cas erreur on renouvelle l'appel  j+1 (on ne stocke pas le clavis)
ch
      nNou

         ?? Est-ce que la mission est modfiable
         ??


api pacte 0::n
[
{
id:
code:
libelle
}
]
     */
}
