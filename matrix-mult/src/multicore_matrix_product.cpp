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

double OnMultLineParallelOuterFor(int m_ar, int m_br)
{
	vector<double> pha(m_ar * m_ar, 1.0), phb(m_ar * m_ar, 1.0), phc(m_ar * m_ar, 0.0);
	for (int i = 0; i < m_ar; i++)
        for (int j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (int i = 0; i < m_br; i++)
        for (int j = 0; j < m_br; j++)
            phb[i * m_br + j] = i + 1;

	int i, k, j;

	chrono::time_point<chrono::system_clock> start, end;
    start = chrono::system_clock::now();

	#pragma omp parallel for private(j, k)
	for (i = 0; i < m_ar; i++)
	{
		for (k = 0; k < m_br; k++)
		{
			for (j = 0; j < m_ar; j++)
			{
				phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
			}
		}
	}

    end = chrono::system_clock::now();
    chrono::duration<double> elapsed_seconds = end - start;

    cout << "Time: " << elapsed_seconds.count() << " seconds" << endl;

	// display 10 elements of the result matrix to verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	return elapsed_seconds.count();
}

double OnMultLineParallelInnerFor(int m_ar, int m_br)
{
	vector<double> pha(m_ar * m_ar, 1.0), phb(m_ar * m_ar, 1.0), phc(m_ar * m_ar, 0.0);
	for (int i = 0; i < m_ar; i++)
        for (int j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (int i = 0; i < m_br; i++)
        for (int j = 0; j < m_br; j++)
            phb[i * m_br + j] = i + 1;

	int i, k, j;

	chrono::time_point<chrono::system_clock> start, end;
    start = chrono::system_clock::now();

	#pragma omp parallel private(i, k)
	for (i = 0; i < m_ar; i++)
	{
		for (k = 0; k < m_br; k++)
		{
			#pragma omp for     // the threads synchronize here
            for (j = 0; j < m_ar; j++)
			{
				phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
			}
		}
	}

	end = chrono::system_clock::now();
    chrono::duration<double> elapsed_seconds = end - start;

    cout << "Time: " << elapsed_seconds.count() << " seconds" << endl;

	// display 10 elements of the result matrix to verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	return elapsed_seconds.count();
}

void runFunctionTests(ofstream &ofs, double (*f)(int, int)) {
	ofs << "matrix_size" << "," << "time" << "," << "num_threads" << endl;
	cout << "Function running" << endl;
	cout << endl;
	for (int mx_size = 600; mx_size <= 3000; mx_size += 400) {
		double timeTaken = f(mx_size, mx_size);
		ofs << mx_size << "," << timeTaken << "," << omp_get_num_procs() << endl;
		cout << "Ran for size " << mx_size << endl;
	}
	for (int mx_size = 4096; mx_size <= 10240; mx_size += 2048) {
		double timeTaken = f(mx_size, mx_size);
		ofs << mx_size << "," << timeTaken << "," << omp_get_num_procs() << endl;
		cout << "Ran for size " << mx_size << endl;
	}
}


void runTests(const string &fileName) {
	cout << "Writing to file " << fileName << endl;
	ofstream ofs(fileName);
	runFunctionTests(ofs, OnMultLineParallelOuterFor);
	runFunctionTests(ofs, OnMultLineParallelInnerFor);
}


int main(int argc, char *argv[]) {
	if (argc > 1) {
		runTests(argv[1]);
		return 0;
	}
	int op, lin, col;
    op = 1;

	do
	{
		cout << endl;
		cout << "1. Line Multiplication Parallel Outer loop" << endl;
        cout << "2. Line Multiplication Parallel Inner loop" << endl;
		cout << "3. Run tests" << endl;
		cout << "Selection?: ";
		cin >> op;
		if (op == 0)
			break;

		if (op == 1 || op == 2) {
			printf("Dimensions: lins=cols ? ");
			cin >> lin;
			col = lin;
		}

		switch (op)
		{
            case 1:
                OnMultLineParallelOuterFor(lin, col);
                break;
            case 2:
                OnMultLineParallelInnerFor(lin, col);
                break;
			case 3:
				runTests("parallel" + to_string(time(0)) + ".csv");
				break;
		}
	} while (op != 0);
}
