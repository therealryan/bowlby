package dev.flowty.bowlby.app.github;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * API message definitions and de/serialisation utilities. Note that these
 * object models are not complete - we're just including those fields that we're
 * interested in.
 */
class Message {
  private static final Logger LOG = LoggerFactory.getLogger( Message.class );
  private static final ObjectMapper JSON = new ObjectMapper()
      .enable( SerializationFeature.INDENT_OUTPUT )
      .enable( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS );

  private Message() {
    // no instances
  }

  /**
   * Dumps an object to json
   *
   * @param o The object
   * @return The serialised form
   */
  public static String toJson( Object o ) {
    try {
      return JSON.writeValueAsString( o );
    }
    catch( JsonProcessingException e ) {
      LOG.error( "Failed to serialise {}", e );
      return "Serialisation failure!";
    }
  }

  /**
   * Parses a jackson-compatible type from the response body
   *
   * @param <T> The parsed type
   */
  private static class JsonBodyHandler<T> implements HttpResponse.BodyHandler<T> {
    private Class<T> type;

    /**
     * @param type the parsed type
     */
    private JsonBodyHandler( Class<T> type ) {
      this.type = type;
    }

    @Override
    public BodySubscriber<T> apply( HttpResponse.ResponseInfo responseInfo ) {
      return HttpResponse.BodySubscribers.mapping(
          BodySubscribers.ofString( StandardCharsets.UTF_8 ),
          body -> {
            try {
              LOG.trace( "Full response {}", body );
              return JSON.readValue( body, type );
            }
            catch( IOException e ) {
              throw new UncheckedIOException( e );
            }
          } );
    }
  }

  /**
   * https://docs.github.com/en/rest/actions/workflow-runs?apiVersion=2022-11-28#list-workflow-runs-for-a-workflow
   */
  @SuppressWarnings("javadoc")
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class ListWorkflowRunResponse {
    static JsonBodyHandler<ListWorkflowRunResponse> HANDLER = new JsonBodyHandler<>(
        ListWorkflowRunResponse.class );

    @JsonProperty("workflow_runs")
    public final List<WorkflowRun> runs;

    public ListWorkflowRunResponse(
        @JsonProperty("workflow_runs") List<WorkflowRun> runs ) {
      this.runs = runs;
    }
  }

  @SuppressWarnings("javadoc")
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class WorkflowRun {
    @JsonProperty("id")
    public final String id;
    @JsonProperty("status")
    public final String status;
    @JsonProperty("conclusion")
    public final String conclusion;
    @JsonProperty("run_started_at")
    public final String startedAt;

    public WorkflowRun(
        @JsonProperty("id") String id,
        @JsonProperty("status") String status,
        @JsonProperty("conclusion") String conclusion,
        @JsonProperty("run_started_at") String startedAt ) {
      this.id = id;
      this.status = status;
      this.conclusion = conclusion;
      this.startedAt = startedAt;
    }
  }

  /**
   * https://docs.github.com/en/rest/actions/artifacts?apiVersion=2022-11-28#list-workflow-run-artifacts
   */
  @SuppressWarnings("javadoc")
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class ListWorkflowRunArtifactsResponse {
    static JsonBodyHandler<ListWorkflowRunArtifactsResponse> HANDLER = new JsonBodyHandler<>(
        ListWorkflowRunArtifactsResponse.class );

    @JsonProperty("artifacts")
    public final List<WorkflowRunArtifact> artifacts;

    public ListWorkflowRunArtifactsResponse(
        @JsonProperty("artifacts") List<WorkflowRunArtifact> artifacts ) {
      this.artifacts = artifacts;
    }
  }

  @SuppressWarnings("javadoc")
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class WorkflowRunArtifact {

    @JsonProperty("id")
    public final String id;
    @JsonProperty("name")
    public final String name;

    public WorkflowRunArtifact(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name ) {
      this.id = id;
      this.name = name;
    }
  }
}
