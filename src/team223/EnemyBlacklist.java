package team223;

import java.util.HashMap;

import team223.EnemyBlacklist.BlacklistEntry;
import battlecode.common.Clock;
import battlecode.common.GameObject;

public final class EnemyBlacklist extends HashMap<Integer,BlacklistEntry> 
{
	public static class BlacklistEntry {
		
		public final GameObject object;
		public final int entryCreatedInRound;
		
		public BlacklistEntry(GameObject object) {
			this.object = object;
			this.entryCreatedInRound = Clock.getRoundNum();
		}
		
		@Override
		public int hashCode() {
			return object.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof BlacklistEntry && ((BlacklistEntry) obj).object.equals( this.object );
		}
		
		@Override
		public String toString() {
			return object.toString();
		}
	}
	
	private final int maxEntryAgeInRounds;
	
	public EnemyBlacklist(int maxEntryAgeInRounds) {
		this.maxEntryAgeInRounds = maxEntryAgeInRounds;
	}

	public void add(GameObject object) {
		if ( MyConstants.DEBUG_MODE ) System.out.println("Blacklisting enemy "+object);
		put( object.getID() , new BlacklistEntry(object) );
	}

	public void removeStaleEntries() 
	{
		if ( ! isEmpty() ) 
		{
			final int currentRound = Clock.getRoundNum();			
			final BlacklistEntry[] copy = values().toArray( new BlacklistEntry[ this.size() ] );
			for ( int i = 0 ; i < copy.length ; i++)
			{
				if ( ( currentRound - copy[i].entryCreatedInRound ) > maxEntryAgeInRounds ) {
					if ( MyConstants.DEBUG_MODE ) System.out.println("removing stale enemy entry from blacklist: "+copy[i]);
					remove( copy[i].object.getID() );
				}
			}
		}
	}
}