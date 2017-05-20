FROM		scholzj/qpid-dispatch:0.8.0
MAINTAINER 	JAkub Scholz "www@scholzj.com"

# Add router configuration
COPY ./qdrouterd.conf /var/lib/qdrouterd/qdrouterd.conf
COPY ./auth-policy /var/lib/qdrouterd/auth-policy/
COPY ./sasl/ /var/lib/qdrouterd/sasl/

# Expose port and run
EXPOSE 5672
CMD ["qdrouterd", "--config", "/var/lib/qpidd/qdrouterd.conf"]
