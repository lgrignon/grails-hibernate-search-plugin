package grails.plugins.hibernate.search

import org.hibernate.Session
import org.hibernate.search.FullTextSession
import org.hibernate.search.MassIndexer
import org.hibernate.search.Search
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import grails.plugins.*;

class HibernateSearchConfig {
	
	private final static Logger log = LoggerFactory.getLogger(HibernateSearchConfig.class);

    private MassIndexer massIndexer
    private final FullTextSession fullTextSession
    private static final List MASS_INDEXER_METHODS = MassIndexer.methods.findAll { it.returnType == MassIndexer }*.name

	boolean throwExceptionOnEmptyQuery
	
    HibernateSearchConfig( Session session ) {
        this.fullTextSession = Search.getFullTextSession( session )
    }

    /**
     *
     * Rebuild the indexes of all indexed entity types with custom config
     *
     */
    def rebuildIndexOnStart( Closure massIndexerDsl ) {

        log.debug "Start rebuilding indexes of all indexed entity types..."

        massIndexer = fullTextSession.createIndexer()

        invokeClosureNode massIndexerDsl

        massIndexer.startAndWait()
    }

    /**
     *
     * Rebuild the indexes of all indexed entity types with default options:
     * - CacheMode.IGNORE
     * - purgeAllOnStart = true
     * - optimizeAfterPurge = true
     * - optimizeOnFinish = true
     *
     */
    def rebuildIndexOnStart( boolean rebuild ) {

        if ( !rebuild )
            return

        log.debug "Start rebuilding indexes of all indexed entity types..."

        massIndexer = fullTextSession.createIndexer().startAndWait()
    }
	
	/**
	 * Throws exception if Hibernate Search raises an EmptyQueryException, (could occur if analyzer has stop words) default false
	 */
	def throwOnEmptyQuery( boolean throwException ) {
		
		log.debug "throwExceptionOnEmptyQuery = " + throwException
		
		throwExceptionOnEmptyQuery = throwException
	}

    Object invokeMethod( String name, Object args ) {
        if ( name in MASS_INDEXER_METHODS ) {
            massIndexer = massIndexer.invokeMethod name, args
        }

        // makes it possible to ignore not concerned config
    }

    def invokeClosureNode( Closure callable ) {
        if ( !callable )
            return

        callable.delegate = this
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        callable.call()
    }

}
