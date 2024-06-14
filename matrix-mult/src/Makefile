CC = g++

SRC_NORMAL = matrix_product.cpp
TARGET_NORMAL = matrix_product
CFLAGS_NORMAL = -lpapi

SRC_MULTICORE = multicore_matrix_product.cpp
TARGET_MULTICORE = multicore_matrix_product
CFLAGS_MULTICORE = -fopenmp -lpapi

both:
	make normal && make multicore

clean:
	rm -f multicore_matrix_product matrix_product

normal:
	$(CC) -O2 $(SRC_NORMAL) -o $(TARGET_NORMAL) $(CFLAGS_NORMAL)

run_normal:
	make normal && ./$(TARGET_NORMAL)

run_normal_tests:
	make normal && ./$(TARGET_NORMAL) normal$$(date +%s%N).csv

multicore:
	$(CC) -O2 $(SRC_MULTICORE) -o $(TARGET_MULTICORE) $(CFLAGS_MULTICORE)

run_multicore:
	make multicore && ./$(TARGET_MULTICORE)
