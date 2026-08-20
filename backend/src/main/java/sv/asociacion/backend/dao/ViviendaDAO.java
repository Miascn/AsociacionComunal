package sv.asociacion.backend.dao;

import sv.asociacion.backend.config.DBConnection;
import sv.asociacion.backend.entity.Vivienda;
import java.sql.*;
import java.util.*;

public class ViviendaDAO {
    private static final String SELECT = """
        SELECT v.*, CONCAT(m.nombres, ' ', m.apellidos) representante,
               SUM(CASE WHEN r.tipo_persona='ADULTO' THEN 1 ELSE 0 END) adultos,
               SUM(CASE WHEN r.tipo_persona='MENOR' THEN 1 ELSE 0 END) menores
        FROM vivienda v LEFT JOIN miembro m ON m.id_miembro=v.id_representante
        LEFT JOIN residente_vivienda r ON r.id_vivienda=v.id_vivienda
        """;

    public List<Vivienda> findAll() {
        List<Vivienda> result = new ArrayList<>();
        try (Connection c=DBConnection.getInstance().getConnection(); Statement s=c.createStatement();
             ResultSet rs=s.executeQuery(SELECT+" GROUP BY v.id_vivienda ORDER BY v.codigo")) {
            while(rs.next()) result.add(map(rs));
        } catch(SQLException e) { throw new IllegalStateException("No fue posible consultar las viviendas.", e); }
        return result;
    }

    public Optional<Vivienda> findById(int id) {
        try (Connection c=DBConnection.getInstance().getConnection();
             PreparedStatement p=c.prepareStatement(SELECT+" WHERE v.id_vivienda=? GROUP BY v.id_vivienda")) {
            p.setInt(1,id); try(ResultSet rs=p.executeQuery()){ return rs.next()?Optional.of(map(rs)):Optional.empty(); }
        } catch(SQLException e) { throw new IllegalStateException("No fue posible consultar la vivienda.", e); }
    }

    public List<Resident> residents(int id) {
        List<Resident> result=new ArrayList<>();
        String sql="SELECT id_residente,id_miembro,nombre_completo,tipo_persona,es_representante FROM residente_vivienda WHERE id_vivienda=? ORDER BY es_representante DESC,nombre_completo";
        try(Connection c=DBConnection.getInstance().getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,id);try(ResultSet rs=p.executeQuery()){while(rs.next())result.add(new Resident(rs.getInt(1),nullableInt(rs.getObject(2)),rs.getString(3),rs.getString(4),rs.getBoolean(5)));}
        }catch(SQLException e){throw new IllegalStateException("No fue posible consultar los residentes.",e);}
        return result;
    }

    public Vivienda create(Vivienda v, List<String> adults, List<String> minors) { return save(v,adults,minors,false); }
    public Vivienda update(Vivienda v, List<String> adults, List<String> minors) { return save(v,adults,minors,true); }

    private Vivienda save(Vivienda v,List<String> adults,List<String> minors,boolean update){
        try(Connection c=DBConnection.getInstance().getConnection()){
            c.setAutoCommit(false);try{
                if(update){
                    try(PreparedStatement p=c.prepareStatement("UPDATE vivienda SET codigo=?,sector=?,direccion=?,referencia=?,id_representante=?,estado=? WHERE id_vivienda=?")){
                        bind(p,v);p.setInt(7,v.getIdVivienda());if(p.executeUpdate()!=1)throw new SQLException("Vivienda inexistente.");
                    }
                }else{
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO vivienda(codigo,sector,direccion,referencia,id_representante,fecha_registro,estado) VALUES(?,?,?,?,?,CURRENT_DATE,?)",Statement.RETURN_GENERATED_KEYS)){
                        bind(p,v);p.executeUpdate();try(ResultSet k=p.getGeneratedKeys()){k.next();v.setIdVivienda(k.getInt(1));}
                    }
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE miembro SET id_vivienda=? WHERE id_miembro=?")){p.setInt(1,v.getIdVivienda());p.setInt(2,v.getIdRepresentante());p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("DELETE FROM residente_vivienda WHERE id_vivienda=?")){p.setInt(1,v.getIdVivienda());p.executeUpdate();}
                String representativeName;
                try(PreparedStatement p=c.prepareStatement("SELECT CONCAT(nombres,' ',apellidos) FROM miembro WHERE id_miembro=? AND estado='ACTIVO'")){p.setInt(1,v.getIdRepresentante());try(ResultSet rs=p.executeQuery()){if(!rs.next())throw new SQLException("Representante inválido.");representativeName=rs.getString(1);}}
                insertResident(c,v.getIdVivienda(),v.getIdRepresentante(),representativeName,"ADULTO",true);
                for(String name:adults)if(!name.isBlank()&&!name.equalsIgnoreCase(representativeName))insertResident(c,v.getIdVivienda(),null,name.trim(),"ADULTO",false);
                for(String name:minors)if(!name.isBlank())insertResident(c,v.getIdVivienda(),null,name.trim(),"MENOR",false);
                c.commit();return findById(v.getIdVivienda()).orElseThrow();
            }catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
        }catch(Exception e){throw new IllegalStateException("No fue posible guardar la vivienda.",e);}
    }

    public void deactivate(int id){try(Connection c=DBConnection.getInstance().getConnection();PreparedStatement p=c.prepareStatement("UPDATE vivienda SET estado='INACTIVA' WHERE id_vivienda=?")){p.setInt(1,id);p.executeUpdate();}catch(SQLException e){throw new IllegalStateException("No fue posible desactivar la vivienda.",e);}}
    private void bind(PreparedStatement p,Vivienda v)throws SQLException{p.setString(1,v.getCodigo());p.setString(2,v.getSector());p.setString(3,v.getDireccion());p.setString(4,v.getReferencia());p.setInt(5,v.getIdRepresentante());p.setString(6,v.getEstado());}
    private void insertResident(Connection c,int house,Integer member,String name,String type,boolean representative)throws SQLException{try(PreparedStatement p=c.prepareStatement("INSERT INTO residente_vivienda(id_vivienda,id_miembro,nombre_completo,tipo_persona,es_representante) VALUES(?,?,?,?,?)")){p.setInt(1,house);if(member==null)p.setNull(2,Types.INTEGER);else p.setInt(2,member);p.setString(3,name);p.setString(4,type);p.setBoolean(5,representative);p.executeUpdate();}}
    private Vivienda map(ResultSet rs)throws SQLException{return new Vivienda(rs.getInt("id_vivienda"),rs.getString("codigo"),rs.getString("sector"),rs.getString("direccion"),rs.getString("referencia"),nullableInt(rs.getObject("id_representante")),rs.getString("representante"),rs.getDate("fecha_registro").toLocalDate(),rs.getString("estado"),rs.getInt("adultos"),rs.getInt("menores"));}
    private Integer nullableInt(Object value){return value==null?null:((Number)value).intValue();}
    public record Resident(int id,Integer memberId,String name,String type,boolean representative){}
}
