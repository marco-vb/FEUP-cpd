#include <chrono>
#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <fstream>
#include <vector>
#include <cstdlib>
#include <time.h>
#include <papi.h>
#include <omp.h>

using namespace std;

double OnMult(int m_ar, int m_br) {
    vector<double> pha(m_ar * m_ar, 1.0), phb(m_ar * m_ar, 1.0), phc(m_ar * m_ar, 0.0);

    for (int i = 0; i < m_ar; i++)
        for (int j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (int i = 0; i < m_br; i++)
        for (int j = 0; j < m_br; j++)
            phb[i * m_br + j] = i + 1;

    chrono::time_point<chrono::system_clock> start, end;
    start = chrono::system_clock::now();

    for (int i = 0; i < m_ar; i++) {
        for (int j = 0; j < m_br; j++) {
            for (int k = 0; k < m_ar; k++) {
                phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
            }
        }
    }

    end = chrono::system_clock::now();
    chrono::duration<double> elapsed_seconds = end - start;

    cout << "Time: " << elapsed_seconds.count() << " seconds" << endl;

    // display 10 elements of the result matrix to verify correctness
    cout << "Result matrix: " << endl;
    for (int i = 0; i < 1; i++) {
        for (int j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    return elapsed_seconds.count();
}

double OnMultLine(int m_ar, int m_br) {
    vector<double> pha(m_ar * m_ar, 1.0), phb(m_ar * m_ar, 1.0), phc(m_ar * m_ar, 0.0);

    for (int i = 0; i < m_ar; i++)
        for (int j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (int i = 0; i < m_br; i++)
        for (int j = 0; j < m_br; j++)
            phb[i * m_br + j] = i + 1;

    chrono::time_point<chrono::system_clock> start, end;
    start = chrono::system_clock::now();

    for (int i = 0; i < m_ar; i++) {
        for (int k = 0; k < m_br; k++) {
            for (int j = 0; j < m_ar; j++) {
                phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
            }
        }
    }

    end = chrono::system_clock::now();
    chrono::duration<double> elapsed_seconds = end - start;

    cout << "Time: " << elapsed_seconds.count() << " seconds" << endl;

    // display 10 elements of the result matrix to verify correctness
    cout << "Result matrix: " << endl;
    for (int i = 0; i < 1; i++) {
        for (int j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    return elapsed_seconds.count();
}

double OnMultBlock(int m_ar, int m_br, int bkSize) {
    vector<double> pha(m_ar * m_ar, 1.0), phb(m_ar * m_ar, 1.0), phc(m_ar * m_ar, 0.0);

    chrono::time_point<chrono::system_clock> start, end;
    start = chrono::system_clock::now();

    for (int i = 0; i < m_ar; i++)
        for (int j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (int i = 0; i < m_br; i++)
        for (int j = 0; j < m_br; j++)
            phb[i * m_br + j] = i + 1;

    for (int x = 0; x < m_ar; x += bkSize) {
        for (int y = 0; y < m_ar; y += bkSize) {
            for (int i = 0; i < m_ar; i++) {
                for (int k = y; k < min(y + bkSize, m_ar); k++) {
                    for (int j = x; j < min(x + bkSize, m_br); j++) {
                        phc[i * m_br + j] += pha[i * m_ar + k] * phb[k * m_br + j];
                    }
                }
            }
        }
    }

    end = chrono::system_clock::now();
    chrono::duration<double> elapsed_seconds = end - start;

    cout << "Time: " << elapsed_seconds.count() << " seconds" << endl;

    cout << "Result matrix: " << endl;
    for (int i = 0; i < 1; i++) {
        for (int j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    return elapsed_seconds.count();
}

void handle_error(int retval) {
    printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
    // exit(1);
}

void stop_and_reset_papi(int eventSet, long long values[2]) {
    int ret = PAPI_stop(eventSet, values);
    if (ret != PAPI_OK) {
        cout << "ERROR: Stop PAPI" << endl;
        handle_error(ret);
    }
    printf("L1 DCM: %lld \n", values[0]);
    printf("L2 DCM: %lld \n", values[1]);

    ret = PAPI_reset(eventSet);
    if (ret != PAPI_OK) {
        std::cout << "FAIL reset" << endl;
        handle_error(ret);
    }
}

void init_papi() {
    int retval = PAPI_library_init(PAPI_VER_CURRENT);
    if (retval != PAPI_VER_CURRENT && retval < 0) {
        printf("PAPI library version mismatch!\n");
        exit(1);
    }
    if (retval < 0)
        handle_error(retval);

    std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
        << " MINOR: " << PAPI_VERSION_MINOR(retval)
        << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}

void run_tests(ofstream& ofs, int start, int end, int jump, int eventSet, long long values[2], double (*multMatrices)(int, int)) {
    int ret;
    for (int sz = start; sz <= end; sz += jump) {
        ret = PAPI_start(eventSet);
        if (ret != PAPI_OK) {
            cout << "ERROR: Start PAPI" << endl;
            handle_error(ret);
        }
        double secondsTaken = multMatrices(sz, sz);
        stop_and_reset_papi(eventSet, values);

        ofs << sz << ',' << secondsTaken << ',' << values[0] << ',' << values[1] << endl;
    }
    ofs << endl;
}

void run_tests_block(ofstream& ofs, int start, int end, int jump, const vector<int>& blockSizes, int eventSet, long long values[2]) {
    int ret;
    ofs << "matrix_size,block_size,time,L1_DCM, L2_DCM\n";
    for (int sz = start; sz <= end; sz += jump) {
        cout << "Matrix size: " << sz << endl;
        for (int bk : blockSizes) {
            cout << "Block size: " << bk << endl;
            ret = PAPI_start(eventSet);
            if (ret != PAPI_OK) {
                cout << "ERROR: Start PAPI" << endl;
                handle_error(ret);
            }
            double secondsTaken = OnMultBlock(sz, sz, bk);
            stop_and_reset_papi(eventSet, values);

            ofs << sz << ',' << bk << ',' << secondsTaken << ',' << values[0] << ',' << values[1] << '\n';
        }
    }
}

void measure_times(const string& fileName, int eventSet, long long values[2]) {
    cout << "Writing to file " << fileName << endl;
    ofstream ofs(fileName);
    ofs << "matrix_size,time,L1_DCM,L2_DCM\n";

    run_tests(ofs, 600, 3000, 400, eventSet, values, OnMult);
    cout << "Completed running basic matrix multiplication tests" << endl;

    run_tests(ofs, 600, 3000, 400, eventSet, values, OnMultLine);
    cout << "Completed running matrix line multiplication tests" << endl;

    run_tests(ofs, 4096, 10240, 2048, eventSet, values, OnMultLine);
    cout << "Completed running line mat mul bigger sizes" << endl;

    run_tests_block(ofs, 4096, 10240, 2048, vector<int>({ 128, 256, 512 }), eventSet, values);
    cout << "Completed running block matrix multiplication tests" << endl;
}

int main(int argc, char* argv []) {
    char c;
    int lin, col, blockSize;
    int op;

    int EventSet = PAPI_NULL;
    long long values[2];
    int ret;

    init_papi();

    ret = PAPI_create_eventset(&EventSet);
    if (ret != PAPI_OK)
    {
    	cout << "ERROR: create eventset" << endl;
    	handle_error(ret);
    }

    ret = PAPI_add_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
    {
    	cout << "ERROR: PAPI_L1_DCM" << endl;
    	handle_error(ret);
    }

    ret = PAPI_add_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
    {
    	cout << "ERROR: PAPI_L2_DCM" << endl;
    	handle_error(ret);
    }

    if (argc > 1) {
        measure_times(argv[1], EventSet, values);
        ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
        if (ret != PAPI_OK)
        	std::cout << "FAIL L1 remove event" << endl;

        ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
        if (ret != PAPI_OK)
        	std::cout << "FAIL L2 remove event" << endl;

        ret = PAPI_destroy_eventset(&EventSet);
        if (ret != PAPI_OK)
        	std::cout << "FAIL destroy" << endl;
        return 0;
    }

    op = 1;
    do {
        cout << "\n1. Multiplication" << endl;
        cout << "2. Line Multiplication" << endl;
        cout << "3. Block Multiplication" << endl;
        cout << "Selection?: ";
        cin >> op;
        if (op == 0)
            break;
        printf("Dimensions: lins=cols ? ");
        cin >> lin;
        col = lin;

        // Start counting
        ret = PAPI_start(EventSet);
        if (ret != PAPI_OK)
        {
        	cout << "ERROR: Start PAPI" << endl;
        	handle_error(ret);
        }

        switch (op) {
        case 1:
            OnMult(lin, col);
            break;
        case 2:
            OnMultLine(lin, col);
            break;
        case 3:
            cout << "Block Size? ";
            cin >> blockSize;
            OnMultBlock(lin, col, blockSize);
            break;
        }

        ret = PAPI_stop(EventSet, values);
        if (ret != PAPI_OK)
        	cout << "ERROR: Stop PAPI" << endl;
        printf("L1 DCM: %lld \n", values[0]);
        printf("L2 DCM: %lld \n", values[1]);

        ret = PAPI_reset(EventSet);
        if (ret != PAPI_OK)
        {
        	std::cout << "FAIL reset" << endl;
        	handle_error(ret);
        }

    } while (op != 0);

    ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
    	std::cout << "FAIL L1 remove event" << endl;

    ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
    	std::cout << "FAIL L2 remove event" << endl;

    ret = PAPI_destroy_eventset(&EventSet);
    if (ret != PAPI_OK)
    	std::cout << "FAIL destroy" << endl;
}
