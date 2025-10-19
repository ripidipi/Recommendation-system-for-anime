package anime_parsing;

public class ParserBackedPersister implements AnimePersister {
    @Override
    public void save(Anime anime) throws Exception {
        Parser.saveAnimeToDB(anime);
    }
}
