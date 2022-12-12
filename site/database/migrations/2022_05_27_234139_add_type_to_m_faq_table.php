<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddTypeToMFaqTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('m_faq', function (Blueprint $table) {
            $table->integer('Fq_type')->comment('Type of FAQ')->after('Fq_name')->default(1)->nullable()->index();
            $table->integer('Fq_order')->comment('Order')->after('Fq_type')->nullable()->index();
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('m_faq', function (Blueprint $table) {
            //
        });
    }
}
