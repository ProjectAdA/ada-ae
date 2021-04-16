package de.ada.restapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataConstants {
	
	public static final List<Map<String, Object>> AUTOMATED_ANALYSIS_TYPES = new ArrayList<Map<String,Object>>();
	public static final Map<String, Object> AUTOMATED_ANALYSIS_LEVEL  = new HashMap<String, Object>();
	
	//TODO Null not allowed in Map.ofEntries
	
//	public static final Map<String, Object> AUTOMATED_ANALYSIS_LEVEL = Map.ofEntries(
//			entry("elementUri","https://github.com/ProjectAdA/ada-va"),
//			entry("id","AnnotationLevel/AutomatedAnalysis"),
//			entry("elementName","Automated Video Analysis"),
//			entry("elementDescription","Annotations generated by the automated video analysis tools."),
//			entry("sequentialNumber",10),
//			entry("elementFullName",null),
//			entry("elementColor",null),
//			entry("elementNumericValue",null),
//			entry("subElements",TYPES)
//	);
	
	static {
		AUTOMATED_ANALYSIS_LEVEL.put("elementUri","https://github.com/ProjectAdA/ada-va");
		AUTOMATED_ANALYSIS_LEVEL.put("id","AnnotationLevel/AutomatedAnalysis");
		AUTOMATED_ANALYSIS_LEVEL.put("elementName","Automated Video Analysis");
		AUTOMATED_ANALYSIS_LEVEL.put("elementDescription","Annotations generated by AdA audio-visual analysis tools.");
		AUTOMATED_ANALYSIS_LEVEL.put("sequentialNumber",10);
		AUTOMATED_ANALYSIS_LEVEL.put("elementFullName",null);
		AUTOMATED_ANALYSIS_LEVEL.put("elementColor",null);
		AUTOMATED_ANALYSIS_LEVEL.put("elementNumericValue",null);
		AUTOMATED_ANALYSIS_LEVEL.put("subElements",AUTOMATED_ANALYSIS_TYPES);
		
		Map<String,Object> sd = new HashMap<String, Object>();
		sd.put("elementUri","http://manpages.ubuntu.com/manpages/focal/man1/shotdetect.1.html");
		sd.put("id","AnnotationType/shotdetect");
		sd.put("elementName","Shotdetect");
		sd.put("elementFullName","Auto | shotdetect");
		sd.put("elementDescription","Automated Shot Detection");
		sd.put("sequentialNumber",100);
		sd.put("elementColor","#cdad00");
		sd.put("elementNumericValue",null);
		sd.put("maxNumericValue",null);
		sd.put("subElements",null);
		AUTOMATED_ANALYSIS_TYPES.add(sd);

		Map<String,Object> of = new HashMap<String, Object>();
		of.put("elementUri","https://opencv.org/");
		of.put("id","AnnotationType/opticalflow");
		of.put("elementName","Optical Flow");
		of.put("elementFullName","Auto | opticalflow");
		of.put("elementDescription","Optical Flow Analysis");
		of.put("sequentialNumber",101);
		of.put("elementColor","#cdad00");
		of.put("elementNumericValue",null);
		of.put("maxNumericValue",null);
		of.put("subElements",null);
		AUTOMATED_ANALYSIS_TYPES.add(of);

		Map<String,Object> asr = new HashMap<String, Object>();
		asr.put("elementUri","https://github.com/facebookresearch/wav2letter/tree/wav2letter-lua");
		asr.put("id","AnnotationType/asr");
		asr.put("elementName","ASR");
		asr.put("elementFullName","Auto | asr");
		asr.put("elementDescription","Automated Speech Recognition");
		asr.put("sequentialNumber",102);
		asr.put("elementColor","#cdad00");
		asr.put("elementNumericValue",null);
		asr.put("maxNumericValue",null);
		asr.put("subElements",null);
		AUTOMATED_ANALYSIS_TYPES.add(asr);

		Map<String,Object> im = new HashMap<String, Object>();
		im.put("elementUri","https://github.com/tensorflow/models/tree/archive/research/im2txt");
		im.put("id","AnnotationType/im2txt");
		im.put("elementName","im2txt");
		im.put("elementFullName","Auto | im2txt");
		im.put("elementDescription","Image Captioning with im2txt");
		im.put("sequentialNumber",103);
		im.put("elementColor","#cdad00");
		im.put("elementNumericValue",null);
		im.put("maxNumericValue",null);
		im.put("subElements",null);
		AUTOMATED_ANALYSIS_TYPES.add(im);

		Map<String,Object> nt = new HashMap<String, Object>();
		nt.put("elementUri","https://github.com/karpathy/neuraltalk2");
		nt.put("id","AnnotationType/neuraltalk2");
		nt.put("elementName","Neuraltalk2");
		nt.put("elementFullName","Auto | neuraltalk2");
		nt.put("elementDescription","Image Captioning with Neuraltalk2");
		nt.put("sequentialNumber",104);
		nt.put("elementColor","#cdad00");
		nt.put("elementNumericValue",null);
		nt.put("maxNumericValue",null);
		nt.put("subElements",null);
		AUTOMATED_ANALYSIS_TYPES.add(nt);

		Map<String,Object> dc = new HashMap<String, Object>();
		dc.put("elementUri","https://github.com/jcjohnson/densecap");
		dc.put("id","AnnotationType/densecap");
		dc.put("elementName","DenseCap");
		dc.put("elementFullName","Auto | densecap");
		dc.put("elementDescription","Dense Image Captioning with densecap");
		dc.put("sequentialNumber",105);
		dc.put("elementColor","#cdad00");
		dc.put("elementNumericValue",null);
		dc.put("maxNumericValue",null);
		dc.put("subElements",null);
		AUTOMATED_ANALYSIS_TYPES.add(dc);

		Map<String,Object> se = new HashMap<String, Object>();
		se.put("elementUri","https://github.com/oaubert/advene/blob/master/lib/advene/plugins/soundenveloppe.py");
		se.put("id","AnnotationType/soundenvelope");
		se.put("elementName","Sound Envelope");
		se.put("elementFullName","Auto | soundenvelope");
		se.put("elementDescription","Sound Envelope with Advene");
		se.put("sequentialNumber",106);
		se.put("elementColor","#cdad00");
		se.put("elementNumericValue",null);
		se.put("maxNumericValue",null);
		se.put("subElements",null);
		AUTOMATED_ANALYSIS_TYPES.add(se);

		Map<String,Object> ynm = new HashMap<String, Object>();
		ynm.put("elementUri","https://research.google.com/audioset/ontology/music.html");
		ynm.put("id","AnnotationType/yamnet_music");
		ynm.put("elementName","YAMNnet Music");
		ynm.put("elementFullName","Auto | yamnet_music");
		ynm.put("elementDescription","Audio Event Detection with YAMNet (Music)");
		ynm.put("sequentialNumber",107);
		ynm.put("elementColor","#cdad00");
		ynm.put("elementNumericValue",null);
		ynm.put("maxNumericValue",null);
		ynm.put("subElements",null);
		AUTOMATED_ANALYSIS_TYPES.add(ynm);

		Map<String,Object> ynsp = new HashMap<String, Object>();
		ynsp.put("elementUri","https://research.google.com/audioset/ontology/speech_1.html");
		ynsp.put("id","AnnotationType/yamnet_speech");
		ynsp.put("elementName","YAMNnet Speech");
		ynsp.put("elementFullName","Auto | yamnet_speech");
		ynsp.put("elementDescription","Audio Event Detection with YAMNet (Speech)");
		ynsp.put("sequentialNumber",108);
		ynsp.put("elementColor","#cdad00");
		ynsp.put("elementNumericValue",null);
		ynsp.put("maxNumericValue",null);
		ynsp.put("subElements",null);
		AUTOMATED_ANALYSIS_TYPES.add(ynsp);

		Map<String,Object> ynsi = new HashMap<String, Object>();
		ynsi.put("elementUri","https://research.google.com/audioset/ontology/silence_1.html");
		ynsi.put("id","AnnotationType/yamnet_silence");
		ynsi.put("elementName","YAMNnet Silence");
		ynsi.put("elementFullName","Auto | yamnet_silence");
		ynsi.put("elementDescription","Audio Event Detection with YAMNet (Silence)");
		ynsi.put("sequentialNumber",109);
		ynsi.put("elementColor","#cdad00");
		ynsi.put("elementNumericValue",null);
		ynsi.put("maxNumericValue",null);
		ynsi.put("subElements",null);
		AUTOMATED_ANALYSIS_TYPES.add(ynsi);

		Map<String,Object> yno = new HashMap<String, Object>();
		yno.put("elementUri","https://research.google.com/audioset/ontology/index.html");
		yno.put("id","AnnotationType/yamnet_other");
		yno.put("elementName","YAMNnet Other");
		yno.put("elementFullName","Auto | yamnet_other");
		yno.put("elementDescription","Audio Event Detection with YAMNet (Other)");
		yno.put("sequentialNumber",110);
		yno.put("elementColor","#cdad00");
		yno.put("elementNumericValue",null);
		yno.put("maxNumericValue",null);
		yno.put("subElements",null);
		AUTOMATED_ANALYSIS_TYPES.add(yno);
	
	}
}
