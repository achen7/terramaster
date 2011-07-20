import	java.util.*;
import	java.io.File;
import	javax.swing.SwingWorker;
import	org.tmatesoft.svn.core.wc.*;
import	org.tmatesoft.svn.core.SVNURL;
import	org.tmatesoft.svn.core.SVNDepth;
import	org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;

// svn --force co http://terrascenery.googlecode.com/svn/trunk/data/Scenery/Terrain/e100n00/e104n00

class Svn extends Thread
{
  SVNClientManager clientManager;
  SVNUpdateClient updateClient;
  SVNStatusClient statusClient;
  String urlBase = "http://terrascenery.googlecode.com/svn/trunk/data/Scenery/";
  String pathBase;

  LinkedList<TileName> syncList;

  Svn() {
    DAVRepositoryFactory.setup();
    clientManager = SVNClientManager.newInstance();
    updateClient = clientManager.getUpdateClient();
    statusClient = clientManager.getStatusClient();
    updateClient.setIgnoreExternals(false);

    syncList = new LinkedList<TileName>();
  }

  // given a 1x1 tile, figure out the parent 10x10 container
  // return the 10/1 path
  private String buildPath(String tile) {
    if (tile.length() < 7) return null;

    // XXX throw an exception
    int lon = Integer.parseInt(tile.substring(1, 4));
    int lat = Integer.parseInt(tile.substring(5));
    char ew = tile.charAt(0);
    char ns = tile.charAt(4);

    int modlon = lon % 10;
    lon -= ew == 'w' && modlon != 0 ? modlon - 10 : modlon;

    int modlat = lat % 10;
    lat -= ns == 's' && modlat != 0 ? modlat - 10 : modlat;

    return String.format("%s%03d%s%02d/%s", ew, lon, ns, lat, tile);
  }

  // XXX synchronized
  void sync(final Collection<TileName> set)
  {
    syncList.addAll(set);

    /*
    int st = 0;
    int end;
    while ((end = list.indexOf(' ', st)) != -1) {
      String path = buildPath(list.substring(st, end));
      if (path != null) {
	checkout("Terrain/"+path);
	checkout("Objects/"+path);
      }
      st = end + 1;
    }
    */
  }

  /*
  SVNStatus stat = statusClient.doStatus(pathBase +
      "Terrain/e140s40/e144s38/"), true);
  System.out.println(stat.getURL());
  */

  private void checkout(String node) {

    // debug
    if (true) {
      System.out.println("checkout "+node);
      try { Thread.sleep(1500); } catch (Exception x) { }
      return;
    }

    try {

    SVNURL url = SVNURL.parseURIDecoded(urlBase + node);
    File f = new File(pathBase + node);

    long rev = updateClient.doCheckout(url, f, SVNRevision.HEAD,
	SVNRevision.HEAD, SVNDepth.INFINITY, true);
    System.out.println("checked out " + rev);

    } catch (Exception x) { System.out.println(x); }

    /*
    private static long checkout( SVNURL url , SVNRevision revision , File destPath , boolean isRecursive ) throws SVNException {
      SVNUpdateClient updateClient = clientManager.getUpdateClient();
      updateClient.setIgnoreExternals( false );
      return updateClient.doCheckout( url , destPath , revision , revision , isRecursive );
    }
    */
  }

  void quit() {
    clientManager.dispose();
  }

  void setScnPath(File f) {
    pathBase = f.getPath() + "/";
  }

  private boolean noquit = true;

  // XXX synchronized
  public void run()
  {
    while (noquit) {
      if (syncList.size() > 0) {
	TileName n = syncList.removeFirst();
	String path = buildPath(n.getName());
	if (path != null) {
	  checkout("Terrain/"+path);
	  checkout("Objects/"+path);
	}
      } else
	try { Thread.sleep(500); } catch (Exception x) { }
    }
  }

}
