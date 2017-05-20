FROM		scholzj/qpid-dispatch:0.8.0
MAINTAINER 	JAkub Scholz "www@scholzj.com"

USER root:root

# Add router configuration
COPY ./qdrouterd.conf /var/lib/qdrouterd/qdrouterd.conf
COPY ./auth-policy /var/lib/qdrouterd/auth-policy/
COPY ./sasl/ /var/lib/qdrouterd/sasl/
COPY ./docker-entrypoint.sh /

WORKDIR /var/lib/qdrouterd

# Expose port and run
EXPOSE 5672
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["qdrouterd"]
