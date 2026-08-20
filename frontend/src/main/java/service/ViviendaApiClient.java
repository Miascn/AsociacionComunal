package service;

import com.fasterxml.jackson.databind.*;
import sv.asociacion.backend.entity.Vivienda;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

public final class ViviendaApiClient {
    private final QaApiConfig config=QaApiConfig.load();
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json=new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);

    public List<Vivienda> findAll() throws Exception {
        HttpResponse<String> response=client.send(request("").GET().build(),HttpResponse.BodyHandlers.ofString());
        ensure(response,200);return Arrays.stream(json.readValue(response.body(),HouseResponse[].class)).map(HouseResponse::entity).toList();
    }
    public HouseDetail find(int id)throws Exception{
        HttpResponse<String> response=client.send(request("/"+id).GET().build(),HttpResponse.BodyHandlers.ofString());ensure(response,200);
        DetailResponse detail=json.readValue(response.body(),DetailResponse.class);return new HouseDetail(detail.vivienda().entity(),detail.residentes());
    }
    public Vivienda create(HouseRequest value)throws Exception{return save(value,null);}
    public Vivienda update(int id,HouseRequest value)throws Exception{return save(value,id);}
    public void deactivate(int id)throws Exception{HttpResponse<String> r=client.send(request("/"+id).DELETE().build(),HttpResponse.BodyHandlers.ofString());ensure(r,204);}
    private Vivienda save(HouseRequest value,Integer id)throws Exception{
        var builder=request(id==null?"":"/"+id).header("Content-Type","application/json");
        HttpRequest req=(id==null?builder.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value))):builder.PUT(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value)))).build();
        HttpResponse<String> r=client.send(req,HttpResponse.BodyHandlers.ofString());ensure(r,id==null?201:200);return json.readValue(r.body(),HouseResponse.class).entity();
    }
    private HttpRequest.Builder request(String suffix){return HttpRequest.newBuilder(URI.create(config.baseUrl()+"/api/viviendas"+suffix)).timeout(Duration.ofSeconds(20)).header("Authorization","Bearer "+config.token()).header("Accept","application/json").header("ngrok-skip-browser-warning","1");}
    private void ensure(HttpResponse<String> r,int expected)throws IOException{if(r.statusCode()!=expected){String message="HTTP "+r.statusCode();try{message=json.readTree(r.body()).path("error").asText(message);}catch(Exception ignored){}throw new IOException(message);}}

    public record HouseRequest(String codigo,String sector,String direccion,String referencia,Integer idRepresentante,String estado,List<String> adultos,List<String> menores){}
    public record Resident(Integer id,Integer memberId,String name,String type,boolean representative){}
    public record HouseDetail(Vivienda vivienda,List<Resident> residentes){}
    private record DetailResponse(HouseResponse vivienda,List<Resident> residentes){}
    private record HouseResponse(Integer id,String codigo,String sector,String direccion,String referencia,Integer idRepresentante,String representante,String fechaRegistro,String estado,int adultos,int menores){
        Vivienda entity(){return new Vivienda(id,codigo,sector,direccion,referencia,idRepresentante,representante,fechaRegistro==null?null:LocalDate.parse(fechaRegistro),estado,adultos,menores);}
    }
}
