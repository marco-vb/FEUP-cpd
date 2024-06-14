package main

import (
	"fmt"
	"time"
	"os"
)

// Return the minimum of two integers
func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}


// Multiply two matrices
func onMult(m_ar, m_br int) float64 {
	var time_start, time_end time.Time
	var pha, phb, phc []float64

	// Allocate memory for the matrices
	pha = make([]float64, m_ar * m_ar)
	phb = make([]float64, m_ar * m_ar)
	phc = make([]float64, m_ar * m_ar)

	// Initialize the matrices, pha to all 1's, phb to [[1, 2, 3, ...], [1, 2, 3, ...], ...]
	for i := 0; i < m_ar; i++ {
		for j := 0; j < m_ar; j++ {
			pha[i * m_ar + j] = float64(1.0)
		}
	}

	for i := 0; i < m_br; i++ {
		for j := 0; j < m_br; j++ {
			phb[i * m_br + j] = float64(i+1)
		}
	}

	// Perform the multiplication and time it
	time_start = time.Now()

	for i := 0; i < m_ar; i++ {
		for j := 0; j < m_br; j++ {
			var temp float64 = 0.0
			for k := 0; k < m_ar; k++ {
				temp += pha[i * m_ar + k] * phb[k * m_br + j]
			}
			phc[i * m_ar + j] = temp
		}
	}

	time_end = time.Now()

	fmt.Printf("Time: %f\n", time_end.Sub(time_start).Seconds())

	fmt.Printf("Result Matrix:\n")
	for i := 0; i < 1; i++ {
		for j := 0; j < min(10, m_br); j++ {
			fmt.Printf("%f ", phc[i * m_ar + j])
		}
		fmt.Printf("\n")
	}

	return time_end.Sub(time_start).Seconds()
}

func onMultLine(m_ar, m_br int) float64 {
	var time_start, time_end time.Time
	var pha, phb, phc []float64

	pha = make([]float64, m_ar * m_ar)
	phb = make([]float64, m_ar * m_ar)
	phc = make([]float64, m_ar * m_ar)

	for i := 0; i < m_ar; i++ {
		for j := 0; j < m_ar; j++ {
			pha[i * m_ar + j] = float64(1.0)
		}
	}

	for i := 0; i < m_br; i++ {
		for j := 0; j < m_br; j++ {
			phb[i * m_br + j] = float64(i+1)
		}
	}

	time_start = time.Now()

	for i := 0; i < m_ar; i++ {
		for k := 0; k < m_ar; k++ {
			for j := 0; j < m_br; j++ {
				phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j]
			}
		}
	}

	time_end = time.Now()

	fmt.Printf("Time: %f\n", time_end.Sub(time_start).Seconds())

	fmt.Printf("Result Matrix:\n")
	for i := 0; i < 1; i++ {
		for j := 0; j < min(10, m_br); j++ {
			fmt.Printf("%f ", phc[i * m_ar + j])
		}
		fmt.Printf("\n")
	}	

	return time_end.Sub(time_start).Seconds()
}

// Write the results to a file
func run_tests() {
	file, err := os.Create("results.txt")
	if err != nil {
		fmt.Printf("Error creating file\n")
		return
	}

	fmt.Printf("Multiplication\n")
	fmt.Printf("---------------\n")
	for i := 600; i <= 3000; i += 400 {
		fmt.Printf("Matrix %d x %d\n", i, i)
		file.WriteString(fmt.Sprintf("%d %f\n", i, onMult(i, i)))
		fmt.Printf("\n")
	}

	fmt.Printf("Line Multiplication\n")
	fmt.Printf("-------------------\n")
	for i := 600; i <= 3000; i += 400 {
		fmt.Printf("Matrix %d x %d\n", i, i)
		file.WriteString(fmt.Sprintf("%d %f\n", i, onMultLine(i, i)))
		fmt.Printf("\n")
	}

	fmt.Printf("Done\n")
}

func main() {
	var lin, col, op int

	for {
		fmt.Printf("1. Multiplication\n")
		fmt.Printf("2. Line Multiplication\n")
		fmt.Printf("3. Run tests\n")
		fmt.Printf("0. Exit\n")
		fmt.Printf("Option: ")
		fmt.Scan(&op)

		if op == 0 {
			break
		}

		fmt.Printf("Dimensions (n x n): ")
		fmt.Scan(&lin)
		col = lin

		switch op {
		case 1:
			onMult(lin, col)
		case 2:
			onMultLine(lin, col)
		case 3:
			run_tests()
		}
	}
}
