package service;

import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import models.ViviendaModel;
import security.SessionManager;

public final class ViviendaApiClient {
    private final QaApiConfig config=QaApiConfig.load();
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json=new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);

    public List<ViviendaModel> findAll() throws Exception {
        HttpResponse<String> response=client.send(request("").GET().build(),HttpResponse.BodyHandlers.ofString());
        ensure(response,200); return Arrays.stream(json.readValue(response.body(),HouseResponse[].class)).map(HouseResponse::model).toList();
    }
    public HouseDetail find(int id)throws Exception{
        HttpResponse<String> response=client.send(request("/"+id).GET().build(),HttpResponse.BodyHandlers.ofString()); ensure(response,200);
        DetailResponse detail=json.readValue(response.body(),DetailResponse.class); return new HouseDetail(detail.vivienda().model(),detail.residentes());
    }
    public ViviendaModel create(HouseRequest value)throws Exception{return save(value,null);}
    public ViviendaModel update(int id,HouseRequest value)throws Exception{return save(value,id);}
    public void deactivate(int id)throws Exception{var response=client.send(request("/"+id).DELETE().build(),HttpResponse.BodyHandlers.ofString());ensure(response,204);}
    private ViviendaModel save(HouseRequest value,Integer id)throws Exception{
        var builder=request(id==null?"":"/"+id).header("Content-Type","application/json");
        HttpRequest request=(id==null?builder.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value))):builder.PUT(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value)))).build();
        var response=client.send(request,HttpResponse.BodyHandlers.ofString());ensure(response,id==null?201:200);return json.readValue(response.body(),HouseResponse.class).model();
    }
    private HttpRequest.Builder request(String suffix){return HttpRequest.newBuilder(URI.create(config.baseUrl()+"/api/viviendas"+suffix)).timeout(Duration.ofSeconds(20)).header("Authorization","Bearer "+SessionManager.getInstance().requireToken()).header("Accept","application/json").header("ngrok-skip-browser-warning","1");}
    private void ensure(HttpResponse<String> response,int expected)throws IOException{if(response.statusCode()!=expected){String message="HTTP "+response.statusCode();try{message=json.readTree(response.body()).path("error").asText(message);}catch(Exception ignored){}throw new IOException(message);}}

    public record HouseRequest(String codigo,String sector,String direccion,String referencia,Integer idRepresentante,String estado,List<String> adultos,List<String> menores){}
    public record Resident(Integer id,Integer memberId,String name,String type,boolean representative){}
    public record HouseDetail(ViviendaModel vivienda,List<Resident> residentes){}
    private record DetailResponse(HouseResponse vivienda,List<Resident> residentes){}
    private record HouseResponse(Integer id,String codigo,String sector,String direccion,String referencia,Integer idRepresentante,String representante,String fechaRegistro,String estado,int adultos,int menores){
        ViviendaModel model(){return new ViviendaModel(id,codigo,sector,direccion,referencia,idRepresentante,representante,fechaRegistro==null?null:LocalDate.parse(fechaRegistro),estado,adultos,menores);}
    }
}
