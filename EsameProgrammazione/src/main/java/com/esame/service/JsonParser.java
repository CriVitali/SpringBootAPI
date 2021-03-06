package com.esame.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.esame.exception.FilterIllegalArgumentException;
import com.esame.exception.FilterNotFoundException;
import com.esame.exception.InternalGeneralException;
import com.esame.model.Record;
import com.esame.service.FilterService;
import com.esame.util.other.Filter;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Rappresenta la classe statica che effettua il parsing ricorsivo su un 
 * oggetto di tipo JSON
 * @author Marco Sebastianelli
 * @author Cristian Vitali
*/

public class JsonParser {

	/**
	 * Effettua il parsing piu esterno, selezionando il valore colonna
	 * e un oggetto valore, che contiene il filtro da applicare al dataset
	 * ed eventualmente il type con cui aggiungere il filtro ai precedenti.
	 * @param filter contiene il JSON con le informazioni sul filtraggio
	 * @return Un ArrayList di Record filtrato
	 * @throws    InternalGeneralException se vengono generati errori generali interni al server.
	 * @throws    FilterNotFoundException se vengono generati errori di filtro non trovato.
	 * @throws    FilterIllegalArgumentException se vengono generati errori di parametro non valido in ingresso al filtro.
	 */

	public static ArrayList<Record> jsonParserColumn(Object filter) 
	throws InternalGeneralException, FilterNotFoundException, FilterIllegalArgumentException{
		
		ArrayList<Record> previousArray = new ArrayList<Record>();
		ArrayList<Record> filteredArray = new ArrayList<Record>();
		HashMap<String, Object> result = new ObjectMapper().convertValue(filter, HashMap.class);
		
		for(Map.Entry<String, Object> entry : result.entrySet()) {
			
			//ad ogni ciclo ripulisce l array "filteredArray"
			//il vecchio filteredArray diventa Garbage (oggetto senza riferimento)
			filteredArray = new ArrayList<Record>();
		    String column = entry.getKey();
		    Object filterParam = entry.getValue();
		    try {
				filteredArray = jsonParserOperator(column, filterParam, previousArray);
			} catch (  SecurityException e) {

				throw new InternalGeneralException("Error in parsing I/O operation");
				
			} 
		    //ripulisce "previousArray" prima di riempirlo con "filteredArray"
		    //il previousArray precedente diventa Garbage (oggetto senza riferimento)
		    previousArray = new ArrayList<Record>();
		    previousArray.addAll(filteredArray);
		}
		return filteredArray;		
	}
	
	/**
	 * Effettua il parsing piu interno, selezionando l operatore e il parametro
	 * per il filtraggio e lancia il filtro corrispondente alla richiesta. 
	 * @param column rappresenta il campo su cui si vuole effettuare il filtraggio.
	 * @param filterParam contiene i parametri di filtraggio.
	 * @param previousArray rappresenta l'ArrayList ottenuto dai filtraggi precedenti relativi 
	 * ad altre colonne.
	 * @return Un ArrayList di Record filtrato
	 * @throws    InternalGeneralException se vengono generati errori generali interni al server.
	 * @throws    FilterNotFoundException se vengono generati errori di filtro non trovato.
	 * @throws    FilterIllegalArgumentException se vengono generati errori di parametro non valido in ingresso al filtro.
	 */
	
	public static ArrayList<Record> jsonParserOperator(String column, 
													   Object filterParam, 
												       ArrayList<Record> previousArray) 
	throws InternalGeneralException, FilterNotFoundException, FilterIllegalArgumentException {
		
		String type="";
		Filter filter;
		ArrayList<Record> filteredArray = new ArrayList<Record>();
		HashMap<String, Object> result = new ObjectMapper().convertValue(filterParam, HashMap.class);
		
		for(Map.Entry<String, Object> entry : result.entrySet()) {
			
		    String operator = entry.getKey();
		    Object value = entry.getValue();
		    // Se operatore è type allora guarda se il valore è 'and' o 'or'
		    // lancia il metodo runfilter corrispondente
		    if(operator.equals("type") || operator.equals("Type")) {
		    	type = (String) value;
		    	if(!(value.equals("and")) && !(value.equals("or"))) {
		    		throw new FilterIllegalArgumentException("'and' o 'or' expected after 'type'");
		    	}
		    	continue;
		    }
		    
		    filter = FilterService.instanceFilter(column, operator, value);
		    switch(type) {
		    
			    case "and":
			    	filteredArray = FilterService.runFilterAND(filter, previousArray);
			    	break;
			    case "or":
			    	filteredArray = FilterService.runFilterOR(filter, previousArray);
			    	break;
			    default:
			    	filteredArray = FilterService.runFilterOR(filter, previousArray);		    	
		    }
		}
		return filteredArray;	
	}
}
