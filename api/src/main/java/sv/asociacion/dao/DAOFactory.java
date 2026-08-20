package sv.asociacion.dao;

public class DAOFactory {
    private static DAOFactory instance;

    private DAOFactory() {}

    public static synchronized DAOFactory getInstance() {
        if (instance == null) {
            instance = new DAOFactory();
        }
        return instance;
    }

    public RolDAO getRolDAO() {
        return new RolDAO();
    }

    public MiembroDAO getMiembroDAO() {
        return new MiembroDAO();
    }

    public UsuarioDAO getUsuarioDAO() {
        return new UsuarioDAO();
    }

    public CargoDAO getCargoDAO() {
        return new CargoDAO();
    }

    public PeriodoDirectivaDAO getPeriodoDirectivaDAO() {
        return new PeriodoDirectivaDAO();
    }

    public MiembroCargoDAO getMiembroCargoDAO() {
        return new MiembroCargoDAO();
    }

    public AportacionDAO getAportacionDAO() {
        return new AportacionDAO();
    }

    public ProyectoDAO getProyectoDAO() {
        return new ProyectoDAO();
    }

    public VotacionDAO getVotacionDAO() {
        return new VotacionDAO();
    }

    public OpcionVotacionDAO getOpcionVotacionDAO() {
        return new OpcionVotacionDAO();
    }

    public VotoDAO getVotoDAO() {
        return new VotoDAO();
    }

    public ReunionDAO getReunionDAO() {
        return new ReunionDAO();
    }

    public AsistenciaDAO getAsistenciaDAO() {
        return new AsistenciaDAO();
    }

    public BitacoraDAO getBitacoraDAO() {
        return new BitacoraDAO();
    }
}
